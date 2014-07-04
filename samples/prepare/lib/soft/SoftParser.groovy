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

package lib.soft

import groovy.transform.CompileStatic

class SoftParser {
    private File file
    private List<SoftEntity> cachedEntities
    private static final long MAX_LINE_SIZE = 1024 * 500 /* 500 KB */
    private static final char CARET = ('^' as char)

    Map<String, Class> entityTypeImplementation = [
            PLATFORM: SoftPlatformEntity,
            SAMPLE:   SoftSampleEntity,
            SERIES:   SoftEntity,
            DATABASE: SoftEntity
    ]

    SoftParser(File f) {
        file = f
    }

    List<SoftEntity> getEntities() {
        if (!cachedEntities) {
            RandomAccessFile raf
            try {
                raf = new RandomAccessFile(file, 'r')
                populateCachedEntities raf
            } finally {
                raf.close()
            }
        }

        cachedEntities
    }

    SoftEntity getEntityOfType(Class clazz) {
        entities.find { clazz.isAssignableFrom(it.getClass()) }
    }

    List<SoftEntity> getEntitiesOfType(Class clazz) {
        entities.findAll { clazz.isAssignableFrom(it.getClass()) }
    }

    @CompileStatic
    private void populateCachedEntities(RandomAccessFile raf) {
        cachedEntities = new LinkedList()

        byte[] buffer = new byte[MAX_LINE_SIZE]

        int read
        int state = 0
        List<Byte> entityName

        int i
        def position = { ->
            raf.filePointer - read + i
        }
        while ((read = raf.read(buffer)) > 0) {
            for (i = 0; i < read; i++) {
                byte v = buffer[i as int]
                //println "${v as char} ($state)"

                switch (state) {
                    case 0:
                        /* after start */
                        if (v != CARET) {
                            throw new IllegalStateException('Expected ^ at beginning of file')
                        }

                        /* break missing intentionally */
                    case 2:
                        /* inside entity */
                        if (v == CARET) {
                            entityName = new ArrayList()
                            state = 1
                        }
                    break

                    case 1:
                        /* in the caret line, after caret */
                        if (v != ('\n' as char)) {
                            entityName << v
                        } else {
                            addEntity(entityName, position() + 1)
                            state = 2
                        }
                    break
                }
            }
        }
    }

    void addEntity(List<Byte> bytes, long position) {
        String data = new String(bytes as byte[], 'UTF-8')

        def split = data.split(' = ', 2) as List
        String type = split[0]
        String name = split[1]

        Class implementation = entityTypeImplementation[type]
        if (!implementation) {
            throw new RuntimeException("I don't know nothing about no '$type' section :(")
        }

        SoftEntity entity  = implementation.newInstance()
        entity.type        = type
        entity.name        = name
        entity.file        = file
        entity.startOffset = position

        cachedEntities << entity
    }
}
