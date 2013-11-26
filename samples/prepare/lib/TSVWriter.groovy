package lib

class TSVWriter {
    private BufferedWriter writer

    TSVWriter(BufferedWriter writer) {
        this.writer = writer
    }

    void leftShift(List list) {
        writer.writeLine(list.collect {
            String s = (it as String) ?: ''
            if (s.contains('"') || s.contains('\t') || s.contains('\n')) {
                "\"${s.replaceAll('"', '""')}\""
            } else {
                s
            }
        }.join('\t'))
    }
}
