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

{
  my @engines = qw( REScalaSpin REScalaSpinWait REScalaSTM REScalaSync scala.rx scala.react SIDUP );

  my $dbh = importCSV('results', ':memory:', 'results');

  plotThreadPerEngine($dbh, 'results', 'fig', @engines);

}


sub plotThreadPerEngine($dbh, $tableName, $targetDir, @engines) {
  mkdir $targetDir;
  my @datasets;
  for my $engine (@engines) {
    my $data = $dbh->selectall_arrayref("SELECT Threads, Score FROM $tableName WHERE [Param: riname] = ? ORDER BY 0 + Threads", undef, $engine);

    push @datasets, Chart::Gnuplot::DataSet->new(
      xdata => [map {$_->[0]} @$data],
      ydata => [map {$_->[1]} @$data],
      title => "$engine",
      style => "linespoints",
    );
  }
  my $chart = Chart::Gnuplot->new(
    output => "$targetDir/result.pdf",
    terminal => "pdf size 8,6",
    title  => "prim",
    xlabel => "Threads",
    logscale => "x 2; set logscale y 10",
    ylabel => "Score",
  );
  $chart->plot2d(@datasets);

}

sub initDB($dbPath, $tableName, @columns) {
  my $dbh = DBI->connect("dbi:SQLite:dbname=". $dbPath,"","",{AutoCommit => 0,PrintError => 1});

  sub typeColumn($columnName) {
    given($columnName) {
      when(["Threads", "Score", 'Score Error (99,9%)', 'Samples', 'Param: depth', 'Param: sources']) { return qq["$columnName" REAL] }
      default { return qq["$columnName"] }
    }
  }

  if($dbh->selectrow_array("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")) {
    $dbh->do("ALTER TABLE $tableName ADD COLUMN ". typeColumn($_) . " DEFAULT NULL") for @columns;
    return $dbh;
  }

  $dbh->do("CREATE TABLE $tableName (" . (join ',', map { typeColumn($_) . " DEFAULT NULL" } @columns) . ')')
    or die "could not create table";
  return $dbh;
}

sub importCSV($folder, $dbPath, $tableName) {
  my @files = glob("$folder/*.csv");
  my @headers = @{csv(in => $files[0])->[0]};
  my $dbh = initDB($dbPath, $tableName, @headers);
  for my $file (@files) {
    my $data = csv(in => $file, headers => 'skip');
    my $sth = $dbh->prepare("INSERT INTO $tableName VALUES (" . (join ",", map {"?"} @headers) . ")");
    for my $row (@$data) {
      s/(?<=\d),(?=\d)/./g for @$row;  # replace , with . in numbers
    }
    $sth->execute(@$_) for @$data;
  }
  $dbh->commit();
  return $dbh;
}
