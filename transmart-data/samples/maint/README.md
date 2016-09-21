This directory contains maintenance files useful for HTTP sites serving studies
for transmart-data. Copy these files to your server.

These are:

* **make\_index**: creates an index file named `datasets_index` in the
  root of the target directory. It scans the target directory recursively for
  files named `<study>_<data type>.tar.xz`. Invoke as:  
   `./make_index <target directory>`.
* **make\_tarballs**: creates the `tar.xz` files that transmart-data expects.
  It expects under the target directory subdirectories whose name is the name
  of the study. Inside these subdirectories, there should be files named `<data
  type>.params`, and (depending on the data type) possibly also a subdirectory
  named `<data type>`, with the actual data. For each study/data type pair, the
  script creates a compressed tarball inside the study directory named
  `<study>_<data type>.tar.xz`. Pass `--force` (or `-f`) to allow overwriting
  of tarballs, `--delete` (or `-d`) to delete the original files after they are
  compressed, `--type` (or `-t`) to limit the action to one data type and
  `--study` (or `-s`) to limit the action to one study. Syntax:  
  `./make_tarballs [-f] [-d] [-s <study>] [-t <type>] <target directory>`.
