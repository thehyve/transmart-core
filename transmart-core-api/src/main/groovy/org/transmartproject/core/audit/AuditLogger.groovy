package org.transmartproject.core.audit

import javax.servlet.http.HttpServletRequest

interface AuditLogger {

    boolean getEnabled()

    def report(Map<String, Object> params, String event, HttpServletRequest request)
}
