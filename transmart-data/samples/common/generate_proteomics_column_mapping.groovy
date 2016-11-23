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
	def cli = new CliBuilder(usage: "generate_proteomics_column_mapping.groovy")
	cli.i 'tsv input file', longOpt: 'input', args: 1, argName: 'file'
	cli.o 'tsv output file; stdout if unspecified', longOpt: 'output', args: 1, argName: 'file'
	def options = cli.parse(args)
	options
}

options = parseOptions()
if (!options) {
    System.exit 1
}

CSVWriter writer = new CSVWriter(new OutputStreamWriter(options.o ? new FileOutputStream(options.o) : System.out, 'UTF-8'), '\t' as char, '\u0000' as char)
inputFile = new File(options.i)
CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(inputFile), 'UTF-8'), '\t' as char)
Integer ifile = 0
try {
	String[] inLine = reader.readNext()
	writer.writeNext(['file_names', 'peptide_identifier', 'intensity_start_value', 'intensity_end_value'] as String[])
	Integer startIndx = null, endIndx
	inLine.eachWithIndex { val, indx ->
		if(val ==~ /(?i)^LFQ\.intensity\.(.+)_.+$/) {
			if(startIndx == null) startIndx = indx
			endIndx = indx
		}
	}
	writer.writeNext([inputFile.name, ++ifile, startIndx, endIndx ] as String[])
} finally {
	reader.close()
	writer.close()
}
