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

{
  my $dbPath = ':memory:';
  my $table = 'results';
  my $csvDir = 'resultStore';
  my $outDir = 'fig';

  my $dbh = DBI->connect("dbi:SQLite:dbname=". $dbPath,"","",{AutoCommit => 0,PrintError => 1});

  my @frameworks = qw( REScalaSpin REScalaSpinWait REScalaSTM REScalaSync scala.rx scala.react SIDUP );
  my @engines = ("synchron", "spinning", "stm", "spinningWait");

  importCSV($csvDir, $dbh, $table);

  mkdir $outDir;

  plotBenchmarksFor($dbh, $table, "$outDir/simple", "riname", $_, "benchmarks.simple.Mapping.local", "benchmarks.simple.Mapping.shared") for @frameworks;
  plotBenchmarksFor($dbh, $table, "$outDir/grid", "riname", $_, "benchmarks.grid.Bench.primGrid") for @frameworks;
  plotBenchmarksFor($dbh, $table, "$outDir/philosophers", "engineName", $_, "benchmarks.philosophers.PhilosopherCompetition.eat") for @engines;
  plotBenchmarksFor($dbh, $table, "$outDir/stacks", "engineName", $_, "benchmarks.dynamic.Stacks.run") for @engines;

}

sub plotBenchmarksFor($dbh, $tableName, $targetDir, $key, $param, @benchmarks) {
  my @datasets;
  for my $benchmark (@benchmarks) {
    my $data = $dbh->selectall_arrayref(
      "SELECT Threads, Score FROM $tableName WHERE [Param: $key] = ? AND Benchmark = ? ORDER BY 0 + Threads",
       undef, $param, $benchmark);
    push @datasets, makeDataset($benchmark, $data);
  }
  plotDatasets($dbh, $tableName, $targetDir, $param, @datasets);
}

sub makeDataset($name, $data) {
  Chart::Gnuplot::DataSet->new(
    xdata => [map {$_->[0]} @$data],
    ydata => [map {$_->[1]} @$data],
    title => $name,
    style => "linespoints",
  );
}

sub plotDatasets($dbh, $tableName, $targetDir, $name, @datasets) {
  mkdir $targetDir;
  my $chart = Chart::Gnuplot->new(
    output => "$targetDir/$name.pdf",
    terminal => "pdf size 8,6",
    title  => $name,
    xlabel => "Threads",
    #logscale => "x 2; set logscale y 10",
    ylabel => "Operations Per Millisecond",
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
  $dbh->do("UPDATE $tableName SET Score = Score * 1000, Unit = 'op/ms' WHERE Unit = 'op/s'");
  $dbh->commit();
  return $dbh;
}
