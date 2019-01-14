# Test Studies

This folder contains test studies ready to be loaded by the transmart copy. Unlike [test_data studies](../test_data/studies) that has to be ported to the transmart copy format at some point.

## How to load test studies

The loading tool uses `tm_cz` user.
Before you load test data make sure you sources the vars file with `TM_CZ_PWD` envirounment variable specified.

To load test studies run follwing make command:
```
	make load
```

The command will make sure you have specified (see `TRANSMART_COPY_VERSION` in the Makefile) transmart copy version in the current directory. If not, it downloads it from the nexus repository.
