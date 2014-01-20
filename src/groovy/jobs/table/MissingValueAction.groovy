package jobs.table

interface MissingValueAction {

    /* null means delete row. Return an empty string to leave the cell empty */
    Object getReplacement(String primaryKey)

    static class ThrowExceptionMissingValueAction implements MissingValueAction {

        Class exceptionClass
        String message

        @Override
        Object getReplacement(String primaryKey) {
            throw exceptionClass.newInstance(message)
        }
    }

    static class ConstantReplacementMissingValueAction implements MissingValueAction {

        Object replacement

        @Override
        Object getReplacement(String primaryKey) {
            replacement
        }
    }

    static class DropRowMissingValueAction implements MissingValueAction {

        @Override
        Object getReplacement(String primaryKey) {
            null
        }
    }

}
