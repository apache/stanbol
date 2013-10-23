#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.NavigationLink;

@Component
@Service(NavigationLink.class)
public class ExampleMenuItem extends NavigationLink {
    
    public ExampleMenuItem() {
        super("${artifactId}/", "Example:${artifactId}", "An Example Service", 300);
    }
    
}
