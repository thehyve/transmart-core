/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-data.
 *
 * Transmart-data is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-data.  If not, see <http://www.gnu.org/licenses/>.
 */

@Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3')
import au.com.bytecode.opencsv.CSVWriter
import au.com.bytecode.opencsv.CSVReader

def parseOptions() {
	def cli = new CliBuilder(usage: "generate_mirna_data_annot_files.groovy")
	cli.i 'input file directory', required: true, longOpt: 'input_dir', args: 1, argName: 'directory'
	cli.a 'tsv annotation output file', required: true, longOpt: 'annotation', args: 1, argName: 'file'
	cli.d 'tsv data output file', required: true, longOpt: 'data', args: 1, argName: 'file'
	cli.p 'Platform id', required: true, longOpt: 'gpl_id', args: 1, argName: 'gpl_id'
        cli.s 'Species; Homo Sapiens if unspecified', longOpt: 'species', args: 1, argName: 'species'
	def options = cli.parse(args)
	options
}

options = parseOptions()
if (!options) {
    System.exit 1
}

def inputDir = new File(options.i)
if(!inputDir.exists()) {
	System.err.println("${inputDir} does not exist.")
	System.exit 1
}
def data = [:]
inputDir.listFiles().each { file ->
	def matcher = file.name =~ /^(.*)_GeneView.txt$/
	if (matcher) {
		def mirnaMeasures = [:]
		CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(file), 'UTF-8'), '\t' as char)
		try {
			//skip first 2 lines
			reader.readNext()
			reader.readNext()
			def line
			while(line = reader.readNext()) {
				mirnaMeasures[line[0]] = line[2]
			}	
		} finally {
			reader.close()
		}
        	data[(matcher[0][1])] = mirnaMeasures
        }
}

def allMirnaIds = new LinkedHashSet()
data.each { entry ->
	allMirnaIds.addAll(entry.value.keySet())
}
species = options.s ?: 'Homo Sapiens'
CSVWriter annotWriter = new CSVWriter(new OutputStreamWriter(new FileOutputStream(options.a), 'UTF-8'), '\t' as char)
CSVWriter dataWriter = new CSVWriter(new OutputStreamWriter(new FileOutputStream(options.d), 'UTF-8'), '\t' as char)
try {
	annotWriter.writeNext(['id_ref', 'mirna_id', 'sn_id', 'platform', 'organism'] as String[])
	dataWriter.writeNext((['id_ref'] + data.keySet()) as String[])
	int idRef = 1
	allMirnaIds.each { mirnaId ->
		annotWriter.writeNext([idRef, mirnaId, null, options.p, species] as String[])
		dataWriter.writeNext(([idRef] + data.collect  { it.value[mirnaId] }) as String[])
		idRef += 1
	}
} finally {
	annotWriter.close()
	dataWriter.close()
}
