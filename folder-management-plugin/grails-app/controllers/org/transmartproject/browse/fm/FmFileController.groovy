package org.transmartproject.browse.fm

class FmFileController {

    def formLayoutService

    def show = {
        def paramMap = params
        def id = params.id
        def file = FmFile.get(id)
        if (!file) {
            render(text: "File with ID " + id + " not found!")
            return
        }

        def layout = formLayoutService.getLayout('file')
        render(plugin: "folder-management", template: "show", model: [file: file, layout: layout])
    }
}
