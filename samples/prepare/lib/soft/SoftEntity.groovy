package lib.soft

import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap

@Grab(group='com.google.guava', module='guava', version='15.0')
class SoftEntity {
    File file
    Long startOffset

    String type,
           name

    Multimap cachedHeaderAttributes

    protected getTableBeginAttribute() {
        null
    }

    Multimap getHeaderAttributes() {
        if (cachedHeaderAttributes == null) {
            populateHeaderAttributes()
        }

        cachedHeaderAttributes
    }

    public Object getAt(String attribute) {
        attribute = attribute.toLowerCase()
        boolean multiValued = false
        if (attribute.endsWith('*')) {
            attribute = attribute[0..-2]
            multiValued = true
        }

        Collection values = headerAttributes.get(attribute)
        if (!values) {
            return multiValued ? [] : null
        }
        if (multiValued) {
            return values
        }
        // not multivalued
        if (values.size() != 1) {
            throw new RuntimeException("Attribute $attribute is multi-valued: $values")
        }

        values.iterator().next()
    }

    private populateHeaderAttributes() {
        cachedHeaderAttributes = new LinkedListMultimap()

        file.withInputStream { InputStream is ->
            is.skip startOffset

            def bufferedReader = new BufferedReader(new InputStreamReader(is))
            String lastLine
            while ((lastLine = bufferedReader.readLine()) != null) {
                if (lastLine.length() == 0) {
                    continue
                }
                if (lastLine[0] != '!') {
                    return
                }

                def split = lastLine.split(' = ', 2)

                cachedHeaderAttributes.put(
                        split[0][1..-1].toLowerCase(),
                        split[1] ?: '')
            }
        }
    }

    SoftTable getTable() {
        if (tableBeginAttribute == null) {
            return null
        }

        FileInputStream stream = new FileInputStream(file)
        stream.skip startOffset
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(stream))

        String lastLine
        List<String> headers = []
        while ((lastLine = reader.readLine()) != null) {
            if (lastLine.length() == 0) {
                continue
            }
            if (lastLine[0] == "#") {
                headers << lastLine.split(' = ', 2)[0][1..-1]
            } else if (lastLine.equalsIgnoreCase("!$tableBeginAttribute")) {
                return new SoftTable(reader, headers)
            } else if (lastLine[0] != '!') {
                return null
            }
        }

        null
    }
}
