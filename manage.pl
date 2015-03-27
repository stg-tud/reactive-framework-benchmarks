#!/usr/bin/perl
use 5.010;

use strict;
use warnings;
use utf8;
no if $] >= 5.018, warnings => "experimental::smartmatch";

use Data::Dumper;

############### CONFIGURABLE PARAMETERS ###############################

my @THREADS = (1,2,4,8);
my @PHILOSOPHERS = (32, 256);

############### END OF CONFIGURABLE PARAMETERS ########################

my $EXECUTABLE = './Benchmarks/target/start';
my $OUTDIR = 'out';
my $RESULTDIR = 'results';
my @ENGINES = qw< synchron parrp stm >;

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
  mkdir $OUTDIR;
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
    for my $layout (qw<alternating>) {
      my $name = "threads-$size-layout-$layout";
      my $program = makeRunString($name,
        {
          p => { # parameters
            engineName => (join ',', @ENGINES),
            philosophers => (join ',', @PHILOSOPHERS),
            layout => $layout,
          },
          si => "false", # synchronize iterations
          wi => 20, # warmup iterations
          w => "1000ms", # warmup time
          f => 2, # forks
          i => 10, # iterations
          r => "1000ms", # time per iteration
          t => $size, #threads
          to => "10s", #timeout
        },
        "philosophers"
      );
      push @runs, {name => $name, program => $program};
    }
  }

  @runs;
}
