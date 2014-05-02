@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader

def parseOptions() {
	def cli = new CliBuilder(usage: "generate_proteomics_annotation.groovy")
	cli.p 'Platform id', required: true, longOpt: 'gpl_id', args: 1, argName: 'gpl_id'
	cli.s 'Species; Homo Sapiens if unspecified', longOpt: 'species', args: 1, argName: 'species'
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
	writer.writeNext(['probsetId', 'uniprotId', 'species', 'gpl_id'] as String[])
	int uniprotColumnPos = inLine.findIndexOf { it.startsWith('First.accession') } ?: 0
	String species = options.s ?: 'Homo Sapiens'
	String gplId = options.p
	while((inLine = reader.readNext()) != null) {
		def uniprotId = inLine[uniprotColumnPos]
		//First column must be probeset.
		writer.writeNext([inLine[0], uniprotId, species, gplId] as String[])
	}
} finally {
	reader.close()
	writer.close()
}
