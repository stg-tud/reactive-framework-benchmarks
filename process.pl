#!/usr/bin/perl
use 5.020;

use strict;
use warnings;
use utf8;
use experimental 'signatures';
no if $] >= 5.018, warnings => "experimental::smartmatch";

use DBI;
use Text::CSV_XS qw( csv );
use Data::Dumper;
use Chart::Gnuplot;
use File::Find;

# averaging results works because the sample sizes are currently always the same
# combining standard deviations seems harder: http://www.burtonsys.com/climate/composite_standard_deviations.html

{
  my $dbPath = ':memory:';
  my $table = 'results';
  my $csvDir = 'resultStore';
  my $outDir = 'fig';

  my $dbh = DBI->connect("dbi:SQLite:dbname=". $dbPath,"","",{AutoCommit => 0,PrintError => 1});

  my @frameworks = qw( REScalaSpin REScalaSTM REScalaSync ); #  scala.rx scala.react SIDUP;
  my @engines = ("synchron", "spinning", "stm");

  importCSV($csvDir, $dbh, $table);

  mkdir $outDir;
  chdir $outDir;

  plotBenchmarksFor($dbh, $table, "simple", "local",
    map { {Title => "$_", "Param: riname" => $_, Benchmark => "benchmarks.simple.Mapping.local"} } @frameworks);
  plotBenchmarksFor($dbh, $table, "simple", "shared",
    map { {Title => "$_", "Param: riname" => $_, Benchmark => "benchmarks.simple.Mapping.shared"} } @frameworks);
  for my $layout (qw<alternating random third block>) {
    plotBenchmarksFor($dbh, $table, "philosophers", $layout,
      map { {Title => $_, "Param: engineName" => $_ , Benchmark =>  "benchmarks.philosophers.PhilosopherCompetition.eat", "Param: layout" => $layout} } @engines);
  }
  plotBenchmarksFor($dbh, $table, "grid", "combined",
    map { {Title => $_, "Param: riname" => $_, Benchmark => "benchmarks.grid.Bench.primGrid" } } @frameworks);
  # plotBenchmarksFor($dbh, $table, "stacks", "combined",
  #   map {{Title => $_, "Param: work" => undef, "Param: engineName" => $_ , Benchmark => "benchmarks.dynamic.Stacks.run" }} @engines);

  plotBenchmarksFor($dbh, $table, "stacksWork", "combined", map {{Title => $_, "Param: work" => 2000, "Param: engineName" => $_ , Benchmark => "benchmarks.dynamic.Stacks.run" }} @engines);

  # plotBenchmarksFor($dbh, $table, "philosophersBackoff", "backoff",
  #   map { {Title => $_,  "Param: engineName" => 'spinning' , Benchmark => "benchmarks.philosophers.PhilosopherCompetition.eat", "Param: spinningBackOff" => $_ } } (0..9) );

}

sub queryThreads($tableName, $graph) {
  my @keys = keys %{$graph};
  my $where = join " AND ", map {qq["$_" = ?]} @keys;
  return "SELECT Threads, avg(Score) FROM $tableName WHERE $where GROUP BY Threads ORDER BY Threads";
}

sub plotBenchmarksFor($dbh, $tableName, $group, $name, @graphs) {
  my @datasets;
  for my $graph (@graphs) {
    my $title = delete $graph->{"Title"};
    push @datasets, queryDataset($dbh, $title // "unnamed", queryThreads($tableName, $graph),  values %{$graph});
  }
  plotDatasets($group, $name, {}, @datasets);
}

sub queryDataset($dbh, $title, $query, @params) {
  my $data = $dbh->selectall_arrayref($query, undef, @params);
  return makeDataset($title, $data) if (@$data);
  say "query for $title had no results: [$query] @params";
  return;
}

sub makeDataset($title, $data) {
  Chart::Gnuplot::DataSet->new(
    xdata => [map {$_->[0]} @$data],
    ydata => [map {$_->[1]} @$data],
    title => $title,
    style => "linespoints",
  );
}

sub plotDatasets($group, $name, $additionalParams, @datasets) {
  mkdir $group;
  unless (@datasets) {
    say "dataset for $group/$name is empty";
    return;
  }
  my $chart = Chart::Gnuplot->new(
    output => "$group/$name.pdf",
    terminal => "pdf size 8,6",
    title  => $name,
    xlabel => "Threads",
    #logscale => "x 2; set logscale y 10",
    ylabel => "Operations Per Millisecond",
    %$additionalParams
  );
  $chart->plot2d(@datasets);
}

sub updateTable($dbh, $tableName, @columns) {

  sub typeColumn($columnName) {
    given($columnName) {
      when(["Threads", "Score", 'Score Error (99,9%)', 'Samples', 'Param: depth', 'Param: sources']) { return qq["$columnName" REAL] }
      default { return qq["$columnName"] }
    }
  }

  if($dbh->selectrow_array("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")) {
    my %knownColumns = map {$_ => 1} @{ $dbh->selectcol_arrayref("PRAGMA table_info($tableName)", { Columns => [2]}) };
    @columns = grep {! defined $knownColumns{$_} } @columns;
    $dbh->do("ALTER TABLE $tableName ADD COLUMN ". typeColumn($_) . " DEFAULT NULL") for @columns;
    return $dbh;
  }

  $dbh->do("CREATE TABLE $tableName (" . (join ',', map { typeColumn($_) . " DEFAULT NULL" } @columns) . ')')
    or die "could not create table";
  return $dbh;
}

sub importCSV($folder, $dbh, $tableName) {
  my @files;
  find(sub {
      push @files, $File::Find::name if $_ =~ /\.csv$/;
    }, $folder);
  for my $file (@files) {
    my @data = @{ csv(in => $file) };
    say $file and next if !@data;
    my @headers = @{ shift @data };
    updateTable($dbh, $tableName, @headers);

    for my $row (@data) {
      s/(?<=\d),(?=\d)/./g for @$row;  # replace , with . in numbers
    }
    my $sth = $dbh->prepare("INSERT INTO $tableName (" . (join ",", map {qq["$_"]} @headers) . ") VALUES (" . (join ',', map {'?'} @headers) . ")");
    $sth->execute(@$_) for @data;
  }
  $dbh->do("UPDATE $tableName SET Score = Score / 1000, Unit = 'ops/ms' WHERE Unit = 'ops/s'");
  $dbh->commit();
  return $dbh;
}
