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

import groovy.sql.Sql
import DatabaseConnection

def parseOptions() {
  def cli = new CliBuilder(usage: "InsertGplInfo.groovy -p platform -t title -o organism -m marker_type")
  cli.p('which platform', required: true, longOpt: "platform", args: 1)
  cli.t('which title', required: true, longOpt: "title", args: 1)
  cli.o('which organism', required: true, longOpt: "organism", args: 1)
  cli.m('which marker_type', required: true, longOpt: "marker_type", args: 1)
  cli.g('which genome_build; defaults to NULL', longOpt: "genome_build", args: 1)
  cli.a('which gene annotation id; defaults to NULL', longOpt: "gene_annotation_id", args: 1)
  def options = cli.parse(args)
  options
}


def alreadyLoaded(platform) {
  sql = DatabaseConnection.setupDatabaseConnection()
  result = sql.firstRow(
    "SELECT platform FROM deapp.de_gpl_info WHERE platform = ?", [platform]
  )

  if (result == null) {
    return false
  } else {
    return true
  }
  sql.close()
}

def insertGplInfo(options) {
  sql = DatabaseConnection.setupDatabaseConnection()
  sql.execute(
      "INSERT INTO deapp.de_gpl_info(platform, title, organism, marker_type, genome_build, gene_annotation_id)" +
      " VALUES (:platform, :title, :organism, :marker_type, :genome_build, :gene_annotation_id)",
      [platform: options.platform, title: options.title,
              organism: options.organism, marker_type: options.marker_type,
              genome_build: options.genome_build ?: null,
              gene_annotation_id: options.gene_annotation_id ?: null ]
  )
  sql.close()
}

options = parseOptions()
if (!options) {
	System.exit 1
}

if (!alreadyLoaded(options.platform)) {
  insertGplInfo(options)
} else {
  println "Platform ${options.platform} already loaded; skipping"
  System.exit 3
}

// vim: et sts=0 sw=2 ts=2 cindent cinoptions=(0,u0,U0
