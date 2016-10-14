import com.recomdata.servlet.GetGenePatternFile
import com.recomdata.transmart.TransmartContextHolder
import org.springframework.boot.context.embedded.ServletRegistrationBean;

beans = {
    applicationContextHolder(TransmartContextHolder) { bean ->
        bean.factoryMethod = 'getInstance'
    }

    genePatternFileServlet(ServletRegistrationBean, new GetGenePatternFile(), "/analysis/getGenePatternFile")
}
