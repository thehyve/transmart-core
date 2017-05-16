/*************************************************************************
 * tranSMART - translational medicine data mart
 *
 * Copyright 2008-2012 Janssen Research & Development, LLC.
 *
 * This product includes software developed at Janssen Research & Development, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software  * Foundation, either version 3 of the License, or (at your option) any later version, along with the following terms:
 * 1.	You may convey a work based on this program in accordance with section 5, provided that you retain the above notices.
 * 2.	You may convey verbatim copies of this program code as you receive it, in any medium, provided that you retain the above notices.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS    * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 ******************************************************************/


package com.recomdata.grails.plugin.gwas

import au.com.bytecode.opencsv.CSVWriter
import grails.transaction.Transactional
import org.transmart.searchapp.SecureObject;
import org.transmart.searchapp.AuthUserSecureAccess;
import org.transmart.searchapp.AuthUser;

@Transactional
class GwasWebService {

    def dataSource

    def grailsApplication
    
    //This will be used as the column delimiter in the method to write the data file below.
    private String valueDelimiter ="\t";


    def final geneLimitsSqlQueryByKeyword = """
	
	SELECT max(snpinfo.pos) as high, min(snpinfo.pos) as low, min(snpinfo.chrom) as chrom FROM SEARCHAPP.SEARCH_KEYWORD
	INNER JOIN bio_marker bm ON bm.BIO_MARKER_ID = SEARCH_KEYWORD.BIO_DATA_ID
	INNER JOIN deapp.de_snp_gene_map gmap ON gmap.entrez_gene_id = bm.PRIMARY_EXTERNAL_ID
	INNER JOIN DEAPP.DE_RC_SNP_INFO snpinfo ON gmap.snp_name = snpinfo.rs_id
	WHERE KEYWORD=? AND snpinfo.hg_version = ?
	"""

    def final geneLimitsSqlQueryById = """
	
	SELECT BIO_MARKER_ID, max(snpinfo.pos) as high, min(snpinfo.pos) as low, min(snpinfo.chrom) as chrom from bio_marker bm
	INNER JOIN deapp.de_snp_gene_map gmap ON gmap.entrez_gene_id = bm.PRIMARY_EXTERNAL_ID
	INNER JOIN DEAPP.DE_RC_SNP_INFO snpinfo ON gmap.snp_name = snpinfo.rs_id
	WHERE BIO_MARKER_ID = ? AND snpinfo.hg_version = ?
	GROUP BY BIO_MARKER_ID
	"""

    def final geneLimitsSqlQueryByEntrez = """

		SELECT BIO_MARKER_ID, max(snpinfo.pos) as high, min(snpinfo.pos) as low, min(snpinfo.chrom) as chrom from deapp.de_snp_gene_map gmap
		INNER JOIN DEAPP.DE_RC_SNP_INFO snpinfo ON gmap.snp_name = snpinfo.rs_id
	    INNER JOIN BIO_MARKER bm ON CAST(gmap.entrez_gene_id as varchar(200)) = bm.PRIMARY_EXTERNAL_ID AND bm.PRIMARY_SOURCE_CODE = 'Entrez'
		WHERE gmap.entrez_gene_id = ? AND snpinfo.hg_version = ?
	    GROUP BY BIO_MARKER_ID
	
	"""

    def final genePositionSqlQuery = """
		SELECT DISTINCT BIO_MARKER_ID, ENTREZ_GENE_ID, BIO_MARKER_NAME, BIO_MARKER_DESCRIPTION FROM deapp.de_snp_gene_map gmap
		INNER JOIN DEAPP.DE_RC_SNP_INFO snpinfo ON gmap.snp_name = snpinfo.rs_id
		INNER JOIN BIO_MARKER bm ON bm.primary_external_id = CAST(gmap.entrez_gene_id as varchar(200))
		WHERE chrom = ? AND pos >= ? AND pos <= ? AND HG_VERSION = ?
	"""
    // changed study_name to accession because GWAVA needs short name.
    def final modelInfoSqlQuery = """
		SELECT baa.bio_assay_analysis_id as id, ext.model_name as modelName, baa.analysis_name as analysisName, be.accession as studyName
		FROM bio_assay_analysis baa
		LEFT JOIN bio_assay_analysis_ext ext ON baa.bio_assay_analysis_id = ext.bio_assay_analysis_id
		LEFT JOIN bio_experiment be ON baa.etl_id = be.accession
		WHERE baa.bio_assay_data_type = ?
	"""
    //added additional query to pull gene strand information from the annotation.
    def final getGeneStrand = '''
		select strand from DEAPP.de_gene_info where gene_source_id=1 and entrez_id=?
	'''

    def final getRecombinationRatesForGeneQuery = '''
        select position,rate
        from biomart.bio_recombination_rates recomb,
        (select CASE WHEN chrom_start between 0 and ? THEN 0 ELSE (chrom_start-?) END s, (chrom_stop+?) e, chrom
        from deapp.de_gene_info g where gene_symbol=? order by chrom_start) geneSub
        where recomb.chromosome=(geneSub.chrom) and position between s and e order by position
    '''

    def final snpSearchQuery = """
        with data_subset as
        (
        select gwas.rs_id rs_id, LOG_P_VALUE, analysis_name||' - '||bio_experiment.accession analysis_name from BIOMART.bio_assay_analysis_gwas gwas
        join biomart.bio_assay_analysis analysis on (gwas.bio_assay_analysis_id=analysis.bio_assay_analysis_id)
        left outer join biomart.bio_assay_analysis_ext bax on analysis.bio_assay_analysis_id = bax.bio_assay_analysis_id
        JOIN biomart.bio_experiment on bio_experiment.accession=analysis.etl_id
        where gwas.bio_assay_analysis_id in (_analysisIds_)
        )
        select * from (select snps.rs_id rs_id, chrom, pos, gene_name as gene, exon_intron, recombination_rate, regulome_score from deapp.de_rc_snp_info snps,
        (select pos+? sta, pos-? sto, chrom c from deapp.de_rc_snp_info
        where
        RS_ID =? and
        hg_version=?) ak
        where pos between sto and sta and hg_version=? and chrom=c ) ann_res
        join data_subset on (data_subset.rs_id=ann_res.rs_id)
"""

    def computeGeneBounds(String geneSymbol, String geneSourceId, String snpSource) {
        def query = geneLimitsSqlQueryByKeyword;

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        //Prepare the SQL statement.
        stmt = con.prepareStatement(query);
        stmt.setString(1, geneSymbol)
        stmt.setString(2, snpSource)

        rs = stmt.executeQuery();

        try{
            if(rs.next()){
                def high = rs.getLong("HIGH");
                def low = rs.getLong("LOW");
                def chrom = rs.getString("CHROM")
                return [low, high, chrom]
            }
        }finally{
            rs?.close();
            stmt?.close();
            con?.close();
        }
    }

    def getGeneByPosition(String chromosome, Long start, Long stop, String snpSource) {
        def query = genePositionSqlQuery;
        def geneQuery = geneLimitsSqlQueryByEntrez;

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;
        def geneStmt, geneInfoStmt,geneRs, geneInfoRs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        //Prepare the SQL statement.
        stmt = con.prepareStatement(query);
        stmt.setString(1, chromosome)
        stmt.setLong(2, start)
        stmt.setLong(3, stop)
        stmt.setString(4, snpSource)
        rs = stmt.executeQuery();

        def results = []

        geneStmt = con.prepareStatement(geneQuery)
        geneInfoStmt = con.prepareStatement(getGeneStrand)

        try {
            while(rs.next()) {

                def entrezGeneId = rs.getLong("ENTREZ_GENE_ID")
                geneInfoStmt.setString(1, entrezGeneId.toString())
                geneInfoRs=geneInfoStmt.executeQuery();
                def strand = 0
                try
                {if (geneInfoRs.next())
                    strand=geneInfoRs.getString("STRAND")
                } finally {
                    geneInfoRs.close();
                }
                log.debug("Gene strand query:" +geneInfoRs)
                geneStmt.setString(1, entrezGeneId.toString())
                geneStmt.setString(2, snpSource)
                geneRs = geneStmt.executeQuery();
                try {
                    if(geneRs.next()) {
                        results.push([
                                rs.getString("BIO_MARKER_ID"),
                                "GRCh37",
                                rs.getString("BIO_MARKER_NAME"),
                                rs.getString("BIO_MARKER_DESCRIPTION"),
                                geneRs.getString("CHROM"),
                                geneRs.getLong("LOW"),
                                geneRs.getLong("HIGH"),
                                strand,
                                0,
                                rs.getLong("ENTREZ_GENE_ID")
                        ])
                    }
                }
                finally {
                    geneRs?.close();
                }
            }

            return results
        }
        finally {
            rs?.close();
            geneRs?.close();
            stmt?.close();
            geneStmt?.close();
            con?.close();
        }

    }

    def getModelInfo(String type) {
        def query = modelInfoSqlQuery;

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        //Prepare the SQL statement.
        stmt = con.prepareStatement(query);
        stmt.setString(1, type)

        rs = stmt.executeQuery();

        def results = []

        try{
            while(rs.next()){
                def id = rs.getLong("ID");
                def modelName = rs.getString("MODELNAME");
                def analysisName = rs.getString("ANALYSISNAME");
                def studyName = rs.getString("STUDYNAME");

                results.push([id, modelName, analysisName, studyName])
            }
            return results;
        }finally{
            rs?.close();
            stmt?.close();
            con?.close();
        }
    }
	
	def getSecureModelInfo(String type,String user) {
		def query = modelInfoSqlQuery;

		//Create objects we use to form JDBC connection.
		def con, stmt, rs = null;

		//Grab the connection from the grails object.
		con = dataSource.getConnection()

		//Prepare the SQL statement.
		stmt = con.prepareStatement(query);
		stmt.setString(1, type)

		rs = stmt.executeQuery();

		def results = []

		try{
			while(rs.next()){
				def id = new BigInteger(rs.getString("ID"));
				def modelName = rs.getString("MODELNAME");
				def analysisName = rs.getString("ANALYSISNAME");
				def studyName = rs.getString("STUDYNAME");
				//def studyId= new BigInteger(rs.getString("study_id"));
				

			
				if (checkSecureStudyAccess(user.toLowerCase(), studyName))
				{
					results.push([
						id,
						modelName,
						analysisName,
						studyName
					])
				}
			}
			return results;
		}finally{
			rs?.close();
			stmt?.close();
			con?.close();
		}
	}
	
	def checkSecureStudyAccess(user, accession)

	{
		log.debug("checking security for the user: "+user)
		def secObjs=getExperimentSecureStudyList()
		if (secObjs!=null)
		{if (!secObjs.containsKey(accession))
			{
					return true;
			}
			else
			{
				def cser=AuthUser.findByUsername(user)
				if (getGWASAccess(accession, cser).equals("Locked"))
					{
						return false;
					}
					else
					{
						return true;
					}
			}}
		return true;
	}
	
	def getGWASAccess (study_id, user) {
		
		//def level=getLevelFromKey(concept_key);
		def admin=false;
		for (role in user.authorities)
		{
			if (isAdminRole(role)) {
				admin=true;
				return 'Admin'; //just set everything to admin and return it all
			}
		}
		if(!admin) //if not admin merge the data from the two maps
		{
			def tokens=getSecureTokensWithAccessForUser(user);
			//tokens.each{ k, v -> log.debug( "${k}:${v}") }
			if(tokens.containsKey("EXP:"+study_id)) //null tokens are assumed to be unlocked
			{
                            return tokens["EXP:"+study_id]; //found access for this token so put in access level
			}
			else {
                            return "Locked"; //didn't find authorization for this token
                        }
			
		}
	
		return null;
	}
	
	def getExperimentSecureStudyList(){
		
		StringBuilder s = new StringBuilder();
		s.append("SELECT so.bioDataUniqueId, so.bioDataId FROM SecureObject so Where so.dataType='Experiment'")
		def t=[:];
		//return access levels for the children of this path that have them
		def results = SecureObject.executeQuery(s.toString());
		for (row in results){
			def token = row[0];
			def dataid = row[1];
			token=token.replaceFirst("EXP:","")
			log.info(token+":"+dataid);
			t.put(token,dataid);
		}
		return t;
	}
	
	def getSecureTokensWithAccessForUser(user) {
		StringBuilder s = new StringBuilder();
		s.append("SELECT DISTINCT ausa.accessLevel, so.bioDataUniqueId FROM AuthUserSecureAccess ausa JOIN ausa.accessLevel JOIN ausa.secureObject so ")
		s.append(" WHERE ausa.authUser IS NULL OR ausa.authUser.id = ").append(user.id)
		def t=[:];
		//return access levels for the children of this path that have them
		def results = AuthUserSecureAccess.executeQuery(s.toString());
		for (row in results){
			def token = row[1];
			def accessLevel = row[0];
			log.trace(token+":"+accessLevel.accessLevelName);
			t.put(token,accessLevel.accessLevelName);
		}
		t.put("EXP:PUBLIC","OWN");
		return t;
	}
	
	def isAdminRole(role){
		return role.authority.equals("ROLE_ADMIN") || role.authority.equals("ROLE_DATASET_EXPLORER_ADMIN");
	}

    def final analysisDataSqlQueryGwas = """
		SELECT gwas.rs_id as rsid, gwas.bio_asy_analysis_gwas_id as resultid, gwas.bio_assay_analysis_id as analysisid, 
    	gwas.p_value as pvalue, gwas.log_p_value as logpvalue, be.accession as studyname, baa.analysis_name as analysisname, 
    	baa.bio_assay_data_type AS datatype, info.pos as posstart, info.chrom as chromosome, info.gene_name as gene,
    	info.exon_intron as intronexon, info.recombination_rate as recombinationrate, info.regulome_score as regulome
		FROM biomart.Bio_Assay_Analysis_Gwas gwas
		LEFT JOIN deapp.de_rc_snp_info info ON gwas.rs_id = info.rs_id
		LEFT JOIN biomart.Bio_Assay_Analysis baa ON baa.bio_assay_analysis_id = gwas.bio_assay_analysis_id
		LEFT JOIN biomart.bio_experiment be ON be.accession = baa.etl_id
		WHERE (info.pos BETWEEN ? AND ?)
		AND chrom = ? AND info.hg_version = ?
    	AND gwas.bio_assay_analysis_id IN (
	"""

    def final analysisDataSqlQueryEqtl = """
		SELECT eqtl.rs_id as rsid, eqtl.bio_asy_analysis_data_id as resultid, eqtl.bio_assay_analysis_id as analysisid,
		eqtl.p_value as pvalue, eqtl.log_p_value as logpvalue, be.title as studyname, baa.analysis_name as analysisname,
		baa.bio_assay_data_type AS datatype, info.pos as posstart, info.chrom as chromosome, info.gene_name as gene,
		info.exon_intron as intronexon, info.recombination_rate as recombinationrate, info.regulome_score as regulome
		FROM biomart.Bio_Assay_Analysis_eqtl eqtl
		LEFT JOIN deapp.de_rc_snp_info info ON eqtl.rs_id = info.rs_id
		LEFT JOIN biomart.Bio_Assay_Analysis baa ON baa.bio_assay_analysis_id = eqtl.bio_assay_analysis_id
		LEFT JOIN biomart.bio_experiment be ON be.accession = baa.etl_id
		WHERE (info.pos BETWEEN ? AND ?)
		AND chrom = ? AND info.hg_version = ?
		AND eqtl.bio_assay_analysis_id IN (
	"""

    //def final intronValues = ["INTRON", "SPLICE_SITE_ACCEPTOR", "SPLICE_SITE_DONOR"]

    def getAnalysisDataBetween(analysisIds, low, high, chrom, snpSource) {
        //Get all data for the given analysisIds that falls between the limits
        def gwasQuery = analysisDataSqlQueryGwas;
        def eqtlQuery = analysisDataSqlQueryEqtl;
        gwasQuery += analysisIds.join(",") + ")"
        eqtlQuery += analysisIds.join(",") + ")"

        def results = []

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        //Prepare the SQL statement.
        stmt = con.prepareStatement(gwasQuery);
        stmt.setLong(1, low)
        stmt.setLong(2, high)
        stmt.setString(3, String.valueOf(chrom))
        stmt.setString(4, snpSource)

        rs = stmt.executeQuery();

        try{
            while(rs.next()){
                results.push(
                        [rs.getString("rsid"),
                                rs.getLong("resultid"),
                                rs.getLong("analysisid"),
                                rs.getDouble("pvalue"),
                                rs.getDouble("logpvalue"),
                                rs.getString("studyname"),
                                rs.getString("analysisname"),
                                rs.getString("datatype"),
                                rs.getLong("posstart"),
                                rs.getString("chromosome"),
                                rs.getString("gene"),
                                rs.getString("intronexon"),
                                rs.getLong("recombinationrate"),
                                rs.getString("regulome")
                        ])
            }
            return results;
        }finally{
            rs?.close();
            stmt?.close();
        }

        //And again for EQTL
        stmt = con.prepareStatement(eqtlQuery);
        stmt.setLong(1, low)
        stmt.setLong(2, high)
        stmt.setString(3, String.valueOf(chrom))
        stmt.setString(4, snpSource)

        rs = stmt.executeQuery();

        try{
            while(rs.next()){
                results.push([rs.getString("rsid"),
                        rs.getLong("resultid"),
                        rs.getLong("analysisid"),
                        rs.getDouble("pvalue"),
                        rs.getDouble("logpvalue"),
                        rs.getString("studyname"),
                        rs.getString("analysisname"),
                        rs.getString("datatype"),
                        rs.getLong("posstart"),
                        rs.getString("chromosome"),
                        rs.getString("gene"),
                        rs.getString("intronexon"),
                        rs.getLong("recombinationrate"),
                        rs.getString("regulome")
                ])
            }
            return results;
        }finally{
            rs?.close();
            stmt?.close();
            con?.close();
        }
    }

    def getRecombinationRatesForGene(String geneSymbol, Long range) {
        def query = getRecombinationRatesForGeneQuery;

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        //Prepare the SQL statement.
        stmt = con.prepareStatement(query);
        stmt.setLong(1, range)
        stmt.setLong(2, range)
        stmt.setLong(3, range)
        stmt.setString(4, geneSymbol)

        rs = stmt.executeQuery();

        def results = []
        try{
            while(rs.next()){
                results.push([rs.getLong("POSITION"), rs.getDouble("RATE")])
            }
            return results

        }finally{
            rs?.close();
            stmt?.close();
            con?.close();
        }
    }

    def snpSearch(analysisIds, Long range, String rsId, String hgVersion) {
        def query = snpSearchQuery;

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        query = query.replace("_analysisIds_", analysisIds.join(","))
        //Prepare the SQL statement.
        stmt = con.prepareStatement(query);
        stmt.setLong(1, range)
        stmt.setLong(2, range)
        stmt.setString(3, rsId)
        stmt.setString(4, hgVersion)
        stmt.setString(5, hgVersion)

        rs = stmt.executeQuery();

        def results = []
        try{
            while(rs.next()){
                results.push([rs.getString("RS_ID"), rs.getString("CHROM"), rs.getLong("POS"), rs.getDouble("LOG_P_VALUE"), rs.getString("ANALYSIS_NAME"),
                rs.getString("gene"), rs.getString("exon_intron"), rs.getDouble("recombination_rate"), rs.getString("regulome_score")])
            }
            return results

        }finally{
            rs?.close();
            stmt?.close();
            con?.close();
        }
    }

    def recombinationRateBySnpQuery = """
WITH snp_info AS (
    SELECT DISTINCT
      pos - ? as low,
      pos + ? as high,
      chrom
    FROM DEAPP.DE_RC_SNP_INFO
    WHERE RS_ID=? and hg_version=?
)
SELECT chromosome, position, rate, map FROM BIOMART.BIO_RECOMBINATION_RATES
WHERE POSITION > (SELECT low FROM snp_info)
AND POSITION < (SELECT high FROM snp_info)
AND CHROMOSOME = (SELECT chrom FROM snp_info) order by position
"""

    def getRecombinationRateBySnp(snp, range, hgVersion) {
        def query = recombinationRateBySnpQuery;

        //Create objects we use to form JDBC connection.
        def con, stmt, rs = null;

        //Grab the connection from the grails object.
        con = dataSource.getConnection()

        //Prepare the SQL statement.
        stmt = con.prepareStatement(query);
        stmt.setLong(1, range)
        stmt.setLong(2, range)
        stmt.setString(3, snp)
        stmt.setString(4, hgVersion)

        rs = stmt.executeQuery();

        def results = []
        try{
            while(rs.next()){
                results.push([rs.getString("chromosome"), rs.getLong("position"), rs.getDouble("rate"), rs.getDouble("map")])
            }
            return results

        }finally{
            rs?.close();
            stmt?.close();
            con?.close();
        }


    }
	
	def createTemporaryDirectory(jobName)
	{
		try {
			def String tempFolderDirectory = grailsApplication.config.RModules.tempFolderDirectory
			//Initialize the jobTmpDirectory.
			def jobTmpDirectory = tempFolderDirectory + File.separator + "${jobName}" + File.separator
			jobTmpDirectory = jobTmpDirectory.replace("\\","\\\\")
			def jobTmpWorkingDirectory = jobTmpDirectory + "workingDirectory"
			
			//Try to make the working directory.
			File jtd = new File(jobTmpWorkingDirectory)
			jtd.mkdirs();
			
			return jobTmpDirectory
			
		} catch (Exception e) {
			throw new Exception('Failed to create Temporary Directories. Please contact an administrator.', e);
		}
	}
	
	/*
	 * Writes a file based on a passed in array of arrays.
	 */
	def writeDataFile(tempDirectory,dataToWrite,fileName)
	{
		//Construct the path to the temporary directory we will do our work in.
		def fullDirectoryPath = tempDirectory + File.separator
		
		//This is the path to the file that we will return from this function.
		def filePath = ""
		
		//Create a new file to write our data to.
		def outputFile = new File(fullDirectoryPath, fileName);
	
		//Create the buffered writer which will write to our data file.
		BufferedWriter bufWriter = new BufferedWriter(new FileWriter(outputFile), 1024 * 64000);
		
		//Initialize a CSVWriter, tab delimited.
		def writer = new CSVWriter(bufWriter, '\t' as char);
		def output = outputFile.newWriter(true)
		
		//Attempt to write the data to the file.
		try
		{
			//Loop through the outside array.
			dataToWrite.each()
			{
				//Loop through the inside array.
				it.each()
				{
					//Write each value to the file.
					output.write(it.toString());
					
					//Write the record delimiter.
					output.write(valueDelimiter);
				}
				
				//Write a new line to the file.
				output.newLine();
			}
			
		} catch(Exception e) {
			throw new Exception('Failed when writing data to file.', e);
		} finally {
			output?.flush();
			output?.close()
			filePath = outputFile?.getAbsolutePath()
		}
		
		return filePath
	}
}
