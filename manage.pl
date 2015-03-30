#!/usr/bin/perl
use 5.010;

use strict;
use warnings;
use utf8;
use English;
no if $] >= 5.018, warnings => "experimental::smartmatch";

use Data::Dumper;

############### CONFIGURABLE PARAMETERS ###############################

my @THREADS = (1, 2, 4, 8);
my @PHILOSOPHER_TABLE_SIZES = (32, 256);
my $JVM_FORKS = 1;
my @ENGINES = qw< synchron parrp stm >;

############### END OF CONFIGURABLE PARAMETERS ########################

my $EXECUTABLE = './Benchmarks/target/start';
if ($OSNAME eq "MSWin32") {
  $EXECUTABLE = "$EXECUTABLE.bat";
}
my $RESULTDIR = 'results';

# stop java from formating numbers with `,` instead of `.`
$ENV{'LANG'} = 'en_US.UTF-8';

my $command = shift @ARGV;

given($command) {
  when ("show") { say Dumper([ makeRuns() ]) }
  when ("init") { init() }
  when ("run") { run() }
};

sub init {
  mkdir $RESULTDIR;
  chdir "Benchmarks";
  system('sbt','clean', 'stage', 'compileJmh');
  chdir "..";
}

sub run {
  my @runs = makeRuns();
  for my $run (@runs) {
    my $prog = $run->{program};
    say "executing $prog";
    system($prog);
  }
}

sub makeRunString {
  my ($name, $args, @benchmarks) = @_;
  my %arguments = %$args;
  my %params = %{delete $arguments{p}};
  my $paramstring =
    (join ' ',
      (map {"-$_ " . $arguments{$_}} keys %arguments),
      (map {"-p $_=" . $params{$_}} keys %params),
      (join " ", @benchmarks)
    );
  "$EXECUTABLE -rf csv -rff \"results/$name-" . time . ".csv\" $paramstring"
}

sub makeRuns {
  my @runs;

  for my $size (@THREADS) {
      my $name = "threads-$size";
      my $program = makeRunString($name,
        {
          p => { # parameters
            engineName => (join ',', @ENGINES),
            philosophers => (join ',', @PHILOSOPHER_TABLE_SIZES),
            layout => "alternating",
          },
          si => "false", # synchronize iterations
          wi => 20, # warmup iterations
          w => "1000ms", # warmup time
          f => $JVM_FORKS, # forks
          i => 10, # iterations
          r => "1000ms", # time per iteration
          t => $size, #threads
          to => "10s", #timeout
        },
        "philosophers"
      );
      push @runs, {name => $name, program => $program};
  }

  @runs;
}
