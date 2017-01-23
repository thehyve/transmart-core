#!/usr/bin/perl

## This is the VCF parser
## It takes a VCF file as input and create several text files to be loaded into tranSMART
use constant { true => 1, false => 0 };
use List::Util qw(first);

# generate list of alleles in the order expected by GL and PL
# For PL we assume diploidy as per v4.1 of the standard
sub generateAlleles
{
	my @ret = ();
	for my $i ( 0..$_[0] ) 
	{ 
		for my $j ( 0..$i ) 
		{ 
			push @ret, $j.'|'.$i;
		}
	}
	return @ret;
}

if ($#ARGV < 6) {
	print "Usage: perl parseVCFintoTextFiles.pl vcf_input_file output_dir datasource dataset_id gpl_id genome_build ETL_user\n";
	print "    vcf_input_file is the VCF file you want to load\n";
	print "    output_dir is a writable directory where the output files will be stored\n";
	print "    datasource is a textual description of the source of the data\n";
	print "    dataset_id is a unique dataset identifier for this dataset\n";
	print "    gpl_id is an identifier for the platform to use. \n";
	print "        A platform for VCF currently only describes the genome build. If unsure, use 'VCF_<genome_build>'\n";
	print "    genome_build is an identifier for the genome build used as a reference.\n";
	print "    ETL_user is a textual description of the person loading the data. For example the initials.\n";
	print "\n";
	print "Example: perl parseVCFintoTextFiles.pl 54genomes_chr17_10genes.vcf /tmp/vcf CGI 54GenomesChr17 VCF_HG19 hg19 HW\n\n";
	exit;
} else {
	our $vcf_input = $ARGV[0]; 
 	our $output_dir = $ARGV[1];
 	our $datasource = $ARGV[2];
 	our $dataset_id = $ARGV[3];
 	our $gpl_id = $ARGV[4];
 	our $genome = $ARGV[5];
 	our $ETL_user = $ARGV[6];
}

## Do Not change anything after this line
our (@t, $rs);

# Oracle expects the date to be specifially formatted
our $ETL_date = `date +FORMAT=%d-%h-%Y`;
$ETL_date =~ s/FORMAT=//;
$ETL_date =~ s/\n//;
our $depth_threshold = 0;	# The depth_threshold is disabled for now
our $refCount = 0;
our $altCount = 0;
our $het = 0;

# Make sure the output dir exists and is writable
if( !-d $output_dir || !-x $output_dir ) {
    print "The output directory $output_dir doesn't exist or is not writable.\n";
    exit 1;
}

# Create a platform for VCF, if it doesn't exist yet
open PLATFORM, "> $output_dir/load_platform.params" or die "Cannot open file: $!";
print PLATFORM "PLATFORM=\"$gpl_id\"\n";
print PLATFORM "PLATFORM_TITLE=\"VCF platform for $genome\"\n";
print PLATFORM "MARKER_TYPE=\"VCF\"\n";
print PLATFORM "GENOME_BUILD=\"$genome\"\n";
print PLATFORM "ORGANISM=\"Homo Sapiens\"\n";
print PLATFORM "export PLATFORM PLATFORM_TITLE MARKER_TYPE GENOME_BUILD ORGANISM\n";
close PLATFORM;

# Make sure the metadata about the dataset is loaded properly.
open DATASET, "> $output_dir/load_variant_dataset.txt" or die "Cannot open file: $!";
print DATASET "$dataset_id\t$datasource\t$ETL_user\t$ETL_date\t$genome\t$gpl_id\t$comment_file\n";
close DATASET;

# Open different files to load the data
open IN, "< $vcf_input" or die "Cannot open file: $!";
open HEADER, "> $output_dir/load_variant_metadata.txt" or die "Cannot open file: $!";
open IDX, "> $output_dir/load_variant_subject_idx.txt" or die "Cannot open file: $!";
open DETAIL, "> $output_dir/load_variant_subject_detail.txt" or die "Cannot open file: $!";
open SUMMARY, "> $output_dir/load_variant_subject_summary.txt" or die "Cannot open file: $!";
open POPULATION_INFO, "> $output_dir/load_variant_population_info.txt" or die "Cannot open file: $!";
open POPULATION_DATA, "> $output_dir/load_variant_population_data.txt" or die "Cannot open file: $!";

resetNext();

our ($aaChange,$codonChange,$effect,$exonID,$class,$biotype,$gene,$impact,$transcriptID);
our $syn = 0;
our $intron = 0;
our ($chr, $pos, $rs, $ref, $alt, $qual, $filter, $info, $format, @samples);
our %infoFields;

# Count several numbers
my %counts = (
	"patients" => 0,
	"variants" => 0,
	"infoFieldsPerVariant" => 0,
	"infoFields" => 0,
	"noGenotypes" => 0
);
my %numGenotypes = (
	"invalid" => 0,
	".." => 0, 
	".0" => 0, "0." => 0, ".x" => 0, "x." => 0, 
	"00" => 0, "0x" => 0, "x0" => 0, "xx" => 0,
	
	"0" => 0, "x" => 0, "." => 0
);

# Generate allele orderings and cache them so that we don't have to create them for each row and sample
# Store it in a hash indexed by number of combinations for the size of REF+ALT (i.e. for A allele in REF and B,C in ALT, we have 3 in total, so we have these combinations
# AA,AB,BB,AC,BC,CC
# since there are six possibilities we use the allele orderings for 0..2
my %alleles = ();
@{$alleles{3}}=generateAlleles(1);
@{$alleles{6}}=generateAlleles(2);
@{$alleles{10}}=generateAlleles(3);


while (<IN>) {
chomp;
	# Header lines are treated separately. They are stored in the metadata table, as well as analysed for INFO fields
	if (/^##(.*)$/) {
		my @headerparts = split( /=/, $1 );
		my ($key,@value) = @headerparts;
		print HEADER join( "\t", $dataset_id, $key, join( "=", @value ) ), "\n";
		
		# However, the info lines are used to populate the population_info table
		# The format should be 
		#   ##INFO=<ID=id,Number=number..>
		if( /^##INFO=\<(.*)\>/ ) {
			# Count the info field
			$counts{infoFields}++;
			
			# Split the info field on ,
			@fields = split( /,/, $1 );
			my %info;
			
			# Loop through all info characteristics and store the data
			for( $j = 0; $j <= $#fields; $j++ ) {
				# Each characteristic must be of the format key=value
				( $key, $value ) = split( /=/, $fields[$j], 2 );
				
				# If the value starts with a ", it should end with a " as well
				# if not, the string is split on a character in the middle of a 
				# textual field. This must be corrected
				if( substr( $value, 0, 1 ) eq "\"" ) {
					while( substr( $value, -1, 1 ) ne "\"" ) {
						# The value doesn't end with a ", so we should also use the next field
						$value = $value . "," . $fields[ $j + 1 ];
						$j++;
					}
					
					$value = substr( $value, 1, -1 );
				}
				
				$info{lc($key)} = $value;
			}
			
			# Only save this info field if ID is present
			if( exists( $info{"id"} ) ) {
				my $type = exists( $info{"type"} ) ? $info{"type"} : "";
				my $number = exists( $info{"number"} ) ? $info{"number"} : ".";
				print POPULATION_INFO join( "\t", 
										$dataset_id, 
										$info{"id"},
										$type,
										$number,
										exists( $info{"description"} ) ? $info{"description"} : "" ), "\n";
										
				$infoFields{$info{"id"}} = $type;
			}
		}
		
		next;
	}
	
	# Check the line for some specific modifiers, and store their values to save later on
	/SNPEFF_AMINO_ACID_CHANGE=(\w\/\w)\;/ and do {
		$aaChange = $1;
	};
	/SNPEFF_AMINO_ACID_CHANGE=(\w\/\*)\;/ and do {
		$aaChange = $1;
	};
	/SNPEFF_AMINO_ACID_CHANGE=(\*\/\w)\;/ and do {
                $aaChange = $1;
        };
	/SNPEFF_CODON_CHANGE=(\w\w\w\/\w\w\w)\;/ and do {
		$codonChange = $1;
	};
	/SNPEFF_EFFECT=(\w+)\;/ and do {
                $effect = $1;
        };
	/SNPEFF_EXON_ID=(\w+)\;/ and do {
		$exonID = $1;
	};
	/SNPEFF_FUNCTIONAL_CLASS=(\w+)\;/ and do {
		$class = $1;
	};
	/SNPEFF_GENE_BIOTYPE=(\w+)\;/ and do {
		$biotype = $1;
	};
	/SNPEFF_GENE_NAME=(\w+)\;/ and do {
		$gene = $1;
	};
	/SNPEFF_IMPACT=(\w+)\;/ and do {
		$impact = $1;
	};
	/SNPEFF_TRANSCRIPT_ID=(\w+)\;/ and do {
		$transcriptID = "";
	};

	if ($effect eq "SYNONYMOUS_CODING") {
		$syn++;
		#resetNext();
		#next;
	}
	if ($effect eq "INTRON") {
		$intron++;
		#resetNext();
		#next;
	}

	# Split the line into separate parts
	($chr, $pos, $rs, $ref, $alt, $qual, $filter, $info, $format, @samples) = split (/\t/);

	# Store a list of subject names into the _idx table 
	if ($pos eq "POS") {
		for ($i = 0; $i <= $#samples; $i++) {
			$subj = $samples[$i];
			$j = $i + 1;
			if (length($subj) > 50) {
				print "ERROR: Subject ID '".$subj."' is too damn long. Maximum length is 50 characters\n";
				exit 1;
			}
			print IDX "$dataset_id\t$subj\t$j\n";
			push @subjects, $subj;
			
			$counts{"patients"}++;
		}
		next;
	}

	## Some chromosome positions are mapped to multiple RS IDs which is OK
	## However, if a chromosme position is mapped to multiple unknown RS IDs (.), we have to exclude the repeating lines
	$location = $chr . ":" . $pos;
	if (defined $rs_saved{$location} ) {
		if ($rs eq "." && $rs_saved{$location} eq ".") {
			# print TEMP1 "$chr\t$pos\t$rs_saved{$location}\t$filter\t$rs\n";
			resetNext();
			next;
		} 
	}
	
	# Count the variant
	$counts{"variants"}++;
	
	# Store the data for this VCF line into the _detail table
	print DETAIL join("^", $dataset_id, $chr, $pos, $rs, $ref, $alt, $qual, $filter, $info, $format, join("\t", @samples) ), "\n";

	# Store details from the info field
	my @infoData = split( /;/, $info );
	$counts{infoFieldsPerVariant} = $counts{infoFieldsPerVariant} + ( scalar @infoData );
	
	for( $j = 0; $j <= $#infoData; $j++ ) {
		# Each info field should have the format KEY=VALUE
		( $key, $value ) = split( /=/, $infoData[$j] );
		
		# Only store information about keys we know from the header
		if( !exists( $infoFields{$key} ) ) {
			print "Skipping info field $key for $chr:$pos because it is not defined in the header fields\n";
			next;
		}
		
		# To store the data later on, we need to find the correct
		# column type
		my $type = lc( $infoFields{$key} );
		
		# Handle special flag value
		if( $type eq "flag" ) {
			$value = "1";
		}
		
		# Split the column, since multiple values are to be expected
		@info = split( /,/, $value );
		
		for( $k = 0; $k <= $#info; $k++ ) {
			# Store the info value.
			$intVal = "\\N";
			$floatVal = "\\N";
			$textVal = "\\N";
		
			if( $type eq "integer" or $type eq "flag" ) {
				$intVal = $info[$k];
			} elsif( $type eq "float" ) {
				$floatVal = $info[$k];

				# if float value has an exponent with 3 digits (i.e. 1e-200) it's probably safe to say it's zero instead
				# this is added to avoid problems with conversion to double when importing in DB
				$floatVal =~ s/[0-9](\.[0-9]+)?e-[1-9][0-9]{2}/0/;

				# if the floatval is a single ., the value is unknown. Storing NULL instead
				$floatVal =~ s/^\.$/\\N/;
			} elsif( $type eq "character" or $type eq "string" ) {
				$textVal = $info[$k];
			} else {
				print "Unknown data type ($type) for info field $key on $chr:$pos\n";
				next;
			} 
			 
			print POPULATION_DATA join("\t", $dataset_id, $chr, $pos, $key, $k, $intVal, $floatVal, $textVal ) .  "\n";
		}
	}

	if (length($ref) == 1 && length($alt) == 1) {
		$variant_type = "SNV";
	} else {
		$variant_type = "DIV";
	}
	
	# Parse the format given at this line, in order to handle the sample info properly
	# We make a hash of the index of a specific entry in the list, so for example
	#
	#		$format = gt:ad:dp
	#
	# then the hash will be:
	#
	#	{ "gt": 0, "ad": 1, "dp": 2 }
	#
	my @sampleParts = split( /:/, $format );
	my %sampleFormat;
	for( $i = 0; $i <= $#sampleParts; $i++ ) {
		$sampleFormat{@sampleParts[$i]} = $i;
	}

	# Also parse the alternatives, as it might be a list of multiples
	@alternatives = split( /\,/, $alt ); 
	
	# Loop through all samples
	for ($i = 0; $i <= $#samples; $i++) {
		unless ($samples[$i] =~ /\.\/\./) {
			# Parse the sample information, based on the format given before
			my @sampleInfo = split (/\:/, $samples[$i]);
			my $reference = false;
			
			# We are interested in the GT, AD and DP values, the others are neglected
			my $gt, $ad, $dp;
			my $ad1, $ad2;

			# Use the GT column if it is present			
			if( exists( $sampleFormat{"GT"} ) ) {
				$gt = $sampleInfo[$sampleFormat{"GT"}];
			} elsif (exists($sampleFormat{"PL"} ) ) {
				# if GT isn't there look for PL (phred likelihood) and use that
				# first we split it into list of likelihood scores for all possible diploidal alleles with the REF and ALTs
				my @plarray = split(',', $sampleInfo[$sampleFormat{"PL"}]);
				# find the most likely genotype (the one with zero phred scale log likelihood)
				my $index = first { @plarray[$_] eq '0' } 0 .. $#plarray;
				if (defined($index)) {
					# if there is zero, set genotype to the one to which the zero belongs
					$gt = $alleles{scalar @plarray}[$index];
				} else {
					# there is no zero, genotype is not sure. For PL we assume diploidy as per v4.1 of the standard
					$gt = '.|.';
				}

			} else {
				print "Can't import samples for line with SNP " . $rs . " (" . $pos . ") because there is no GT nor PL column present.\n";
				exit;
			}
			if( exists( $sampleFormat{"DP"} ) ) {
				$dp = $sampleInfo[$sampleFormat{"DP"}];
			} else {
				# If the line doesn't contain a DP value, 
				# use the sample anyway, this is achieved by
				# setting the depth to "."
				$dp = "."; 
			}
			if( exists( $sampleFormat{"AD"} ) ) {
				$ad = $sampleInfo[$sampleFormat{"AD"}];
				($ad1, $ad2) = split (/\,/, $ad);
			} else {
				# If the line doesn't contain a AD value,
				# we take half of the depth value for both
				$ad1 = $dp / 2;
				$ad2 = $ad1; 
			}
			
			$diff = abs ($ad1 + $ad2 - $depth);

			# Skip if genotype is not specified
			if( $gt eq "." ) {
				$counts{noGenotypes}++;
   				# print "Can't import sample " . $i . " for line with SNP " . $rs . " (" . $pos . "), because its genotype is not specified.\n";
   				next;
   			}

	     	# Only proceed if the read depth if larger than a set treshold
	     	# or if the read depth is not specified 
	     	if ($dp eq "." or $dp >= $depth_threshhold) {
	     		# Check whether the GT is parseable. That means it should contain a / or a |
	     		# or is numeric altogether (only one allele)
	     		if( $gt =~ m/^[0-9\.]+$/ ) {
	     			$allele = $gt;
	     			# One of the alleles is unknown
					if( $allele eq "." ) {
						$numGenotypes{"."}++;	     			
	     				print "Can't import sample " . $i . " for line with SNP " . $rs . " (" . $pos . "), because the genotype is unknown (" . $gt . ").\n";
	     				next;
	     			}
	     			
	     			# Compute counts for statistics
	     			if( $allele eq "0" ) {
	     				# Both alleles have the reference genotype
	     				$reference = true;
	     			}
	     			
	     			my $countVar = "";
	     			if( $allele eq "0" ) {
	     				$countVar = $countVar . "0";
	     			} elsif( $allele eq "." ) {
	     				$countVar = $countVar . ".";
	     			} else {
	     				$countVar = $countVar . "x";
	     			}
					$numGenotypes{$countVar}++;	     			
	     				
	     			# Determine the variant and variant format
	     			$variant = "";
	     			$variant_format = "";
	     			
	     			if( $allele eq "0" ) {
	     				$variant = $ref;
	     				$variant_format = "R";
	     			} else {
	     				$variant = $alternatives[$allele1 - 1];
	     				$variant_format = "V";
	     			}

					print SUMMARY join("\t", $chr, $pos, $dataset_id, $subjects[$i], $rs, $variant, $variant_format, ( $reference ? "t" : "f" ), $variant_type, $allele, "\\N"), "\n";
	     		} elsif( $gt =~ m/[\/|]/ ) {
     				# The genotype is phased if both alleles are separated by a |, instead of a /
     				$phased = $gt =~ m/\|/; 
     				$alleleSeparator = ( $phased ? "|" : "/" );
     				
	     			($allele1, $allele2) = split( /[\/|]/, $gt );
	     			
	     			# Compute counts for statistics
	     			my $countVar = "";
	     			if( $allele1 eq "0" ) {
	     				$countVar = $countVar . "0";
	     			} elsif( $allele1 eq "." ) {
	     				$countVar = $countVar . ".";
	     			} else {
	     				$countVar = $countVar . "x";
	     			}
	     			if( $allele2 eq "0" ) {
	     				$countVar = $countVar . "0";
	     			} elsif( $allele2 eq "." ) {
	     				$countVar = $countVar . ".";
	     			} else {
	     				$countVar = $countVar . "x";
	     			}	    
					
					$numGenotypes{$countVar}++;
					
	     			# Check if we have a reference or non-reference genotype
	     			# ./0 or 0/. are treated as reference
	     			if( $countVar eq "00" or $countVar eq ".0" or $countVar eq "0." ) {
	     				$reference = true;
	     			}
	     				
	     			# Determine the variant and variant format
	     			$variant = "";
	     			$variant_format = "";
	     			
	     			if( $allele1 eq "0" ) {
	     				$variant = $ref;
	     				$variant_format = "R";
	     			} else {
	     				$variant = $alternatives[$allele1 - 1];
	     				$variant_format = "V";
	     			}
	     			
	     			$variant = $variant . $alleleSeparator;
	     			$variant_format = $variant_format . $alleleSeparator;
	     			
	     			if( $allele2 eq "0" ) {
	     				$variant = $variant . $ref;
	     				$variant_format = $variant_format . "R";
	     			} else {
	     				$variant = $variant . $alternatives[$allele1 - 1];
	     				$variant_format = $variant_format . "V";
	     			}

					print SUMMARY join("\t", $chr, $pos, $dataset_id, $subjects[$i], $rs, $variant, $variant_format, ( $reference ? "t" : "f" ), $variant_type, ( $allele1 eq "." ? "\\N" : $allele1 ), ( $allele2 eq "." ? "\\N" : $allele2 )), "\n";
	     			
	     		} else {
					$numGenotypes{"invalid"}++;
     				print "Can't import sample " . $i . " for line with SNP " . $rs . " (" . $pos . "), because the GT column doesn't contain a / or a | (" . $gt . ").\n";
	     		}
			} else {
				$numGenotypes{"invalid"}++;
   				print "Don't import sample " . $i . " for line with SNP " . $rs . " (" . $pos . "), because the read depth (" . $dp . ") is below the treshold (" . $depth_threshhold . ").\n";
			}
	    } else {
			$numGenotypes{".."}++;
	    }
	 }

    if( $counts{"variants"} % 10000 == 0) {
        print ".";
    }

    if( $counts{"variants"} % 500000 == 0 ) {
        print "\n";
    }

	$rs_saved{$location} = $rs;
	resetNext();
}
close IN;
close OUT;
close POPULATION_INFO;
close POPULATION_DATA;

print "\n";
print "----------------------------------------------------\n";
while ( my ($var, $count) = each(%counts) ) {
	if( $count > 0 ) {
    	print "Count for $var => $count\n";
    }
}
print "\n";
while ( my ($var, $count) = each(%numGenotypes) ) {
	if( $count > 0 ) {
    	print "Count for $var genotypes => $count\n";
    }
}

print "\n";
print "Synonymous coding change: $syn\n";
print "Within intron: $intron\n";
# print "Low depth of coverage: $lowDepth\n";
print "----------------------------------------------------\n";

sub resetNext {
        $effect = "";
        $gt = "";
	$gene = "";
	$geneID = "";
	$strand = "";
	$maf = "";
	$chr = "";
	$pos = "";
	$rs = "";
	$ref = "";
	$alt = "";
	$variant = "";
	$variant_format = "";
	$class = "";
	$biotype = "";
	$impact = "";
	$depth = "";
	$transcriptID = "";
	$exonID = "";
	$aaChange = "";
	$codonChange = "";
}	

