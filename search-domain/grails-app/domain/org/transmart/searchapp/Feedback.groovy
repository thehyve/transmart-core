package org.transmart.searchapp

class Feedback {
    Long id
    Long searchUserId
    Date createDate
    String feedbackText
    String appVersion

    static mapping = {
        table 'SEARCH_USER_FEEDBACK'
        version false
        id generator: 'sequence', params: [sequence: 'SEQ_SEARCH_DATA_ID']
        columns {
            id column: 'SEARCH_USER_FEEDBACK_ID'
            searchUserId column: 'SEARCH_USER_ID'
            createDate column: 'CREATE_DATE'
            feedbackText column: 'FEEDBACK_TEXT'
            appVersion column: 'APP_VERSION'
        }
    }


    static constraints = {
        searchUserId(nullable: true)
    }

}