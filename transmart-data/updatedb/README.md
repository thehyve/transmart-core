Updating DB from one release to the next.
-----------------------------------------

Each floder in this directory contains instructions and helper files for
updating the database to be compatible with the release version indicated in
the directory name. In each case, the top level folder contains a README.md
and the contained folders are postgres, oracle, and common. The directory postgres
for those files that are particular to postgres, the directory oracle for those
files that run on oracle, ane the folder common for files that do not depend on 
the type of database being upgraded. 

NOTE: in all cases the upgrade is for one version step only. That is, for example,
the folder labeled release-16.1 is for an upgrade from 1.2.4 to 16.1, a folder labeled
release-16.2 will be for the upgrade from 16.1 to 16.2, and so on.
