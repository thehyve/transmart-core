import com.recomdata.transmart.TransmartContextHolder;

beans = {
    applicationContextHolder(TransmartContextHolder) { bean ->
        bean.factoryMethod = 'getInstance'
    }
}