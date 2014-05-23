@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader

def parseOptions() {
	def cli = new CliBuilder(usage: "generate_proteomics_annotation.groovy")
	cli.i 'tsv input file', longOpt: 'input', args: 1, argName: 'file'
	cli.o 'tsv output file; stdout if unspecified', longOpt: 'output', args: 1, argName: 'file'
	def options = cli.parse(args)
	options
}

options = parseOptions()
if (!options) {
    System.exit 1
}

CSVWriter writer = new CSVWriter(new OutputStreamWriter(options.o ? new FileOutputStream(options.o) : System.out, 'UTF-8'), '\t' as char)
inputFile = new File(options.i)
CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputFile), 'UTF-8'), '\t' as char)
try {
	String[] inLine = reader.readNext()
	writer.writeNext(['file_names', 'intensity_start_value', 'intensity_end_value'] as String[])
	Integer startIndx = null, endIndx
	inLine.eachWithIndex { val, indx ->
		if(val ==~ /(?i)^LFQ\.intensity\.(.+)_.+$/) {
			if(startIndx == null) startIndx = indx
			endIndx = indx
		}
	}
	writer.writeNext([inputFile.name, startIndx, endIndx ] as String[])
} finally {
	reader.close()
	writer.close()
}
