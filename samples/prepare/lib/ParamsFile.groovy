package lib

class ParamsFile {

    File file
    Map<String,String> params

    void write() {
        file.withWriter {
            params.each { k, v ->
                it << "$k='$v'\n"
            }
        }
    }
}
