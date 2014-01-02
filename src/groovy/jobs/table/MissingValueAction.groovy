package jobs.table

interface MissingValueAction {

    /* null means delete row. Return an empty string to leave the cell empty */
    String getReplacement(String primaryKey)

    static class ThrowExceptionMissingValueAction implements MissingValueAction {

        Class exceptionClass
        String message

        @Override
        String getReplacement(String primaryKey) {
            throw exceptionClass.newInstance(message)
        }
    }

    static class ConstantReplacementMissingValueAction implements MissingValueAction {

        String replacement

        @Override
        String getReplacement(String primaryKey) {
            replacement
        }
    }

    static class DropRowMissingValueAction implements MissingValueAction {

        @Override
        String getReplacement(String primaryKey) {
            null
        }
    }

}
