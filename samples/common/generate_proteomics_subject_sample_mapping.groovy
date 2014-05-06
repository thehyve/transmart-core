@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader

def parseOptions() {
	def cli = new CliBuilder(usage: "generate_proteomics_annotation.groovy")
	cli.p 'Platform id', required: true, longOpt: 'gpl_id', args: 1, argName: 'gpl_id'
	cli.t 'Trial id', required: true, longOpt: 'trial_id', args: 1, argName: 'trial_id'
	cli.y 'Tissue type', required: true, longOpt: 'tissue', args: 1, argName: 'tissue'
	cli.i 'tsv input file; stdin if unspecified', longOpt: 'input', args: 1, argName: 'file'
	cli.o 'tsv output file; stdout if unspecified', longOpt: 'output', args: 1, argName: 'file'
	def options = cli.parse(args)
	options
}

options = parseOptions()
if (!options) {
    System.exit 1
}

CSVWriter writer = new CSVWriter(new OutputStreamWriter(options.o ? new FileOutputStream(options.o) : System.out, 'UTF-8'), '\t' as char)
CSVReader reader = new CSVReader(new InputStreamReader(options.i ? new FileInputStream(options.i) : System.in, 'UTF-8'), '\t' as char)
try {
	String[] inLine = reader.readNext()
	writer.writeNext(['trial_name', 'site_id', 'subject_id', 'sample_cd', 'platform', 'tissue_type', 'attr1', 'attr2', 'cat_cd', 'src_cd'] as String[])
	String gplId = options.p
	String trialId = options.t
	String tissue = options.y
	def lfqs = inLine.collect { it =~ /(?i)^LFQ\.intensity\.(.+)_(.+)$/ }
	lfqs.each { matcher ->
		if(matcher) {
			//TODO site_id is really used in calculations. Needs to be not null value
			writer.writeNext(
				[trialId, 'NA', matcher[0][1], matcher[0][0], gplId, tissue, "LFQ-${matcher[0][2]}", null, 'Biomarker_Data+PLATFORM+ATTR1', 'STD' ] as String[])
		}
	}
} finally {
	reader.close()
	writer.close()
}
