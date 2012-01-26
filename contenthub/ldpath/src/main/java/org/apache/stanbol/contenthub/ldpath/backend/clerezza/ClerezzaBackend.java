package org.apache.stanbol.contenthub.ldpath.backend.clerezza;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NoConvertorException;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TypedLiteral;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.util.W3CDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.api.backend.RDFBackend;

/**
 * @author anil.sinaci
 * 
 */
public class ClerezzaBackend implements RDFBackend<Resource> {

    private static final Logger logger = LoggerFactory.getLogger(ClerezzaBackend.class);

    private static final String XSD = "http://www.w3.org/2001/XMLSchema#";
    final private static String xsdInteger = xsd("integer");
    final private static String xsdInt = xsd("int");
    final private static String xsdShort = xsd("short");
    final private static String xsdByte = xsd("byte");
    final private static String xsdLong = xsd("long");
    final private static String xsdDouble = xsd("double");
    final private static String xsdFloat = xsd("float");
    final private static String xsdAnyURI = xsd("anyURI");
    final private static String xsdDateTime = xsd("dateTime");
    final private static String xsdBoolean = xsd("boolean");
    final private static String xsdString = xsd("string");

    final private static String xsd(String name) {
        return XSD + name;
    }

    private MGraph mGraph;

    public ClerezzaBackend(MGraph mGraph) {
        this.mGraph = mGraph;
    }

    @Override
    public Resource createLiteral(String content) {
        logger.debug("creating literal with content \"{}\"", content);
        return LiteralFactory.getInstance().createTypedLiteral(content);
    }

    private Object getTypedObject(String content, String type) {
        Object obj = content;
        if (type.toString().equals(xsdInteger)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdInt)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdShort)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdByte)) {
            obj = Integer.valueOf(content);
        } else if (type.toString().equals(xsdLong)) {
            obj = Long.valueOf(content);
        } else if (type.toString().equals(xsdDouble)) {
            obj = Double.valueOf(content);
        } else if (type.toString().equals(xsdFloat)) {
            obj = Float.valueOf(content);
        } else if (type.toString().equals(xsdAnyURI)) {
            obj = new UriRef(content);
        } else if (type.toString().equals(xsdDateTime)) {
            DateFormat dateFormat = new W3CDateFormat();
            try {
                obj = dateFormat.parse(content);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (type.toString().equals(xsdBoolean)) {
            obj = Boolean.valueOf(content);
        } else if (type.toString().equals(xsdString)) {
            obj = content;
        }
        return obj;
    }

    @Override
    public Resource createLiteral(String content, Locale language, URI type) {
        logger.debug("creating literal with content \"{}\", language {}, datatype {}",
            new Object[] {content, language, type});
        if (language == null && type == null) {
            return createLiteral(content);
        } else if (type == null) {
            return new PlainLiteralImpl(content, new Language(language.getLanguage()));
        } else {
            return LiteralFactory.getInstance().createTypedLiteral(getTypedObject(content, type.toString()));
        }
    }

    @Override
    public Resource createURI(String uriref) {
        return new UriRef(uriref);
    }

    @Override
    public Double doubleValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Double.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
    }

    @Override
    public Locale getLiteralLanguage(Resource resource) {
        if (resource instanceof PlainLiteral) {
            if (((PlainLiteral) resource).getLanguage() != null) {
                return new Locale(((PlainLiteral) resource).getLanguage().toString());
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a PlainLiteral");
        }
    }

    @Override
    public URI getLiteralType(Resource resource) {
        if (resource instanceof TypedLiteral) {
            if (((TypedLiteral) resource).getDataType() != null) {
                try {
                    return new URI(((TypedLiteral) resource).getDataType().getUnicodeString());
                } catch (URISyntaxException e) {
                    logger.error("TypedLiteral datatype was not a valid URI: {}",
                        ((TypedLiteral) resource).getDataType());
                    return null;
                }
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Value " + resource.toString() + " is not a literal");
        }
    }

    @Override
    public boolean isBlank(Resource resource) {
        return resource instanceof BNode;
    }

    @Override
    public boolean isLiteral(Resource resource) {
        return resource instanceof Literal;
    }

    @Override
    public boolean isURI(Resource resource) {
        return resource instanceof UriRef;
    }

    @Override
    public Collection<Resource> listObjects(Resource subject, Resource property) {
        if (!isURI(property) || !(isURI(subject) || isBlank(subject))) {
            throw new IllegalArgumentException("Subject needs to be a URI or blank node, property a URI node");
        }

        Set<Resource> result = new HashSet<Resource>();
        Iterator<Triple> triples = mGraph.filter((UriRef) subject, (UriRef) property, null);
        while (triples.hasNext()) {
            result.add(triples.next().getObject());
        }

        return result;
    }

    @Override
    public Collection<Resource> listSubjects(Resource property, Resource object) {
        if (!isURI(property)) {
            throw new IllegalArgumentException("Property needs to be a URI node");
        }

        Set<Resource> result = new HashSet<Resource>();
        Iterator<Triple> triples = mGraph.filter(null, (UriRef) property, object);
        while (triples.hasNext()) {
            result.add(triples.next().getSubject());
        }

        return result;
    }

    @Override
    public Long longValue(Resource resource) {
        if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Long.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
    }

    @Override
    public String stringValue(Resource resource) {
        if (resource instanceof UriRef) {
            return ((UriRef) resource).getUnicodeString();
        } else if (resource instanceof Literal) {
            return ((Literal) resource).getLexicalForm();
        } else {
            return resource.toString();
        }
    }

	@Override
	public Boolean booleanValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Boolean.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public Date dateTimeValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Date.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public Date dateValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Date.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public Date timeValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Date.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public Float floatValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Float.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public Integer intValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(Integer.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public BigInteger integerValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
            return LiteralFactory.getInstance().createObject(BigInteger.class, (TypedLiteral) resource);
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}

	@Override
	public BigDecimal decimalValue(Resource resource) {
		if (resource instanceof TypedLiteral) {
			try {
				return LiteralFactory.getInstance().createObject(BigDecimal.class, (TypedLiteral) resource);
			}
			catch(NoConvertorException e) {
				throw new NumberFormatException("Resource " + resource.toString() + " can not converted, no convertor for the BigDecimal");
			}
            
        } else {
            throw new IllegalArgumentException("Resource " + resource.toString() + " is not a TypedLiteral");
        }
	}
}