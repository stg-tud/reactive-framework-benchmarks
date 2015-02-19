#!/usr/bin/perl
use 5.010;

use strict;
use warnings;
use utf8;
use Data::Dumper;

my $EXECUTABLE = './Benchmarks/target/start';
my $OUTDIR = 'out';
my $RESULTDIR = 'results';



given($ARGV[0]) {
  when (undef) { say Dumper(makeRuns()) }
  when ("init") { init(); }
  when ("run") { run(); }
};

sub init {
  mkdir $RESULTDIR;
  chdir "Benchmarks";
  system('sbt','clean', 'stage', 'compileJmh');
  chdir "..";
}

sub run {
  my @runs = @{makeRuns()};
  for my $run (@runs) {
    my $prog = $run->{program};
    say "executing $prog";
    system($prog);
  }
}

sub makeRuns {
  my @runs;

  for my $tablesize (64, 256) {
    for my $layout ("alternating") {
      for my $work (0) {
        for my $threads (1) {
          my $name = "size$tablesize-threads$threads";
          my $program = makeRunString($name,
            {
              p => { # parameters
                philosophers => $tablesize,
                #engine => "pessimistic",
                work => $work,
                layout => $layout,
                tableType => "static",
              },
              wi => 5, # warmup iterations
              w => "2000ms", # warmup time
              f => 1, # forks
              i => 5, # iterations
              r => "1000ms", # time per iteration
              t => $threads, #threads
            },
            ".*reference"
          );
          push @runs, {name => $name, program => $program};
        }
      }
    }
  }

  \@runs;
}

sub submit {
  my ($job) = @_;
  open (my $BSUB, "|-", "bsub");
  print $BSUB $job;
  close $BSUB;
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
  "$EXECUTABLE -rf csv -rff \"results/$name.csv\" $paramstring"
}


sub hhlrjob {
  my ($name, $programstring) = @_;
  return  <<ENDPROGRAM;
#!/bin/sh
# Job name
#BSUB -J REScalaBenchmark
#
# File / path where STDOUT will be written, the %J is the job id
#BSUB -o "$OUTDIR/$name.out"
#
# File / path where STDERR will be written, the %J is the job id
# #BSUB -e REScalaBenchmark.err%J
#
# Request the time you need for execution in minutes
# The format for the parameter is: [hour:]minute,
# that means for 80 minutes you could also use this: 1:20
#BSUB -W 0:30
#
# Request vitual memory you need for your job in MB
#BSUB -M 2048
#
# Request the number of compute slots you want to use
#BSUB -n 16
## BSUB -q testmem
# request exclusive access
#BUSB -x
#
# Specify the MPI support
# #BSUB -a openmpi
#
# Specify your mail address - for activation replace < your email> and remove prepending "# "
# #BSUB -u <your email>
#
# Send a mail when job is done - for activation remove prepending "# "
# #BSUB -N
#

# Unloading a predefined module
module unload openmpi
# Loading the required module
module load java
# Give an overview about all load modules
# module list

# See typical batch-system environment variables - for activation remove character "#"
#export| grep -i LSB

# Space holder - not necessary
echo "--------- processors ---------"
nproc
echo "------------------------------"

# Simple "hello world" program output - Should be generated three times in the output file
export JAVA_OPTS="-Xmx1024m -Xms1024m"
$programstring

# Extended example with reportings for processor/thread binding - for activation remove character "#"
ENDPROGRAM
}