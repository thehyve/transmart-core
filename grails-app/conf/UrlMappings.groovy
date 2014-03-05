class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.${format})?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "/Testtoto/getHistory"(controller:"Testtoto", action:"getHistory")
        "/RetrieveData/createDataLibrary"(controller:"RetrieveData", action:"createDataLibrary")
        "/RetrieveData/seeDataContainedInLibrary"(controller:"RetrieveData", action:"seeDataContainedInLibrary")
	}
}
