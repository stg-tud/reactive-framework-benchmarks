#!/usr/bin/perl
use 5.010;

use strict;
use warnings;
use utf8;
no if $] >= 5.018, warnings => "experimental::smartmatch";

use Data::Dumper;

my $EXECUTABLE = './Benchmarks/target/start';
my $OUTDIR = 'out';
my $RESULTDIR = 'results';
my @FRAMEWORKS = ("REScalaSpin", "REScalaSpinWait", "REScalaSTM", "REScalaSync", "SIDUP", "scala.rx", "scala.react");
my @ENGINES = qw< synchron spinning stm spinningWait >;

# stop java from formating numbers with `,` instead of `.`
$ENV{'LANG'} = 'en_US.UTF-8';

my $command = shift @ARGV;
my @RUN = @ARGV ? @ARGV : qw< prim simple philosophers dynamicStacks >;

say "selected @RUN";

given($command) {
  when ("show") { say Dumper([ makeRuns() ]) }
  when ("init") { init() }
  when ("run") { run() }
  when ("submit") { submitAll() }
};

sub init {
  mkdir $RESULTDIR;
  mkdir $OUTDIR;
  mkdir "$RESULTDIR/$_" for @RUN;
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

sub submitAll {
  for my $run (makeRuns()) {
    submit(hhlrjob($run->{name}, $run->{program}))
  }
}

sub submit {
  my ($job) = @_;
  open (my $BSUB, "|-", "bsub");
  print $BSUB $job;
  close $BSUB;
}

sub makeRunString {
  my ($prefix, $name, $args, @benchmarks) = @_;
  my %arguments = %$args;
  my %params = %{delete $arguments{p}};
  my $paramstring =
    (join ' ',
      (map {"-$_ " . $arguments{$_}} keys %arguments),
      (map {"-p $_=" . $params{$_}} keys %params),
      (join " ", @benchmarks)
    );
  "$EXECUTABLE -rf csv -rff \"results/$prefix/$name.csv\" $paramstring"
}

sub makeRuns {
  my @runs;
  for my $run (@RUN) {
    push @runs, selectRun($run);
  }
  @runs;
}

sub selectRun {
  my ($run) = @_;
  my %selection = (
    simple => sub {
      my @runs;

      for my $size (1..16,32,64) {
        for my $framework (@FRAMEWORKS) {
          my $name = "threads-$size-framework-$framework";
          my $program = makeRunString("simple", $name,
            {
              p => { # parameters
                riname => $framework,
              },
              si => "false", # synchronize iterations
              wi => 20, # warmup iterations
              w => "1000ms", # warmup time
              f => 5, # forks
              i => 10, # iterations
              r => "1000ms", # time per iteration
              t => $size, #threads
              to => "10s", #timeout
            },
            "simple.Mapping"
          );
          push @runs, {name => $name, program => $program};
        }
      }

      @runs;
    },

    prim => sub {
      my @runs;

      for my $size (1..16,32,64) {
        for my $framework (@FRAMEWORKS) {
          my $name = "size-$size-framework-$framework";
          my $program = makeRunString("prim", $name,
            {
              p => { # parameters
                depth => 64,
                sources => 64,
                riname => $framework,
              },
              si => "false", # synchronize iterations
              wi => 20, # warmup iterations
              w => "1000ms", # warmup time
              f => 5, # forks
              i => 10, # iterations
              r => "1000ms", # time per iteration
              t => $size, #threads
              to => "10s", #timeout
            },
            ".*prim"
          );
          push @runs, {name => $name, program => $program};
        }
      }

      @runs;
    },

    philosophers => sub {
      my @runs;

      for my $size (1..16,32,64) {
        for my $engine (@ENGINES) {
          my $name = "threads-$size-engine-$engine";
          my $program = makeRunString("philosophers", $name,
            {
              p => { # parameters
                tableType => 'static',
                engineName => $engine,
                philosophers => "64,256",
              },
              si => "false", # synchronize iterations
              wi => 20, # warmup iterations
              w => "1000ms", # warmup time
              f => 5, # forks
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
    },

    dynamicStacks => sub {
      my @runs;

      for my $size (1..16,32,64) {
        for my $engine (@ENGINES) {
          my $name = "threads-$size-engine-$engine";
          my $program = makeRunString("dynamicStacks", $name,
            {
              p => { # parameters
                engineName => $engine,
                work => 2000,
              },
              si => "false", # synchronize iterations
              wi => 20, # warmup iterations
              w => "1000ms", # warmup time
              f => 5, # forks
              i => 10, # iterations
              r => "1000ms", # time per iteration
              t => $size, #threads
              to => "10s", #timeout
            },
            "dynamic.Stacks"
          );
          push @runs, {name => $name, program => $program};
        }
      }

      @runs;
    },

    reference => sub {
      my @runs;

      for my $size (1..16,32,64) {
          my $name = "threads-$size";
          my $program = makeRunString("reference", $name,
            {
              p => { # parameters
                work => 2000,
              },
              si => "false", # synchronize iterations
              wi => 5, # warmup iterations
              w => "1000ms", # warmup time
              f => 5, # forks
              i => 5, # iterations
              r => "1000ms", # time per iteration
              t => $size, #threads
              to => "10s", #timeout
            },
            "WorkReference"
          );
          push @runs, {name => $name, program => $program};
      }

      @runs;
    },

  );

  if (defined $selection{$run}) {
    return $selection{$run}->();
  }
  else {
    say "unknown: $run";
    return ();
  }

}



sub hhlrjob {
  my ($name, $programstring) = @_;
  return  <<ENDPROGRAM;
#!/bin/sh
# Job name
#BSUB -J REScalaBenchmark
#
# File / path where STDOUT will be written, the %J is the job id
#BSUB -o $OUTDIR/$name-%J.out
#
# File / path where STDERR will be written, the %J is the job id
# #BSUB -e $OUTDIR/$name-%J.err
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
#BSUB -q deflt
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
ls -al /work/local

export JAVA_OPTS="-Xmx1024m -Xms1024m" # -Djava.io.tmpdir=\$TMP
$programstring

# Extended example with reportings for processor/thread binding - for activation remove character "#"
ENDPROGRAM
}
