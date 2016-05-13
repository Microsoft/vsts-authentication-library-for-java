// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.helpers;

import com.microsoft.alm.secret.Token;
import com.microsoft.alm.secret.TokenPair;
import com.microsoft.alm.secret.TokenType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class XmlHelper {
    // Adapted from http://docs.oracle.com/javase/tutorial/jaxp/dom/readingXML.html
    public static String getText(final Node node) {
        final StringBuilder result = new StringBuilder();
        if (!node.hasChildNodes()) return "";

        final NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            if (subnode.getNodeType() == Node.TEXT_NODE) {
                result.append(subnode.getNodeValue());
            } else if (subnode.getNodeType() == Node.CDATA_SECTION_NODE) {
                result.append(subnode.getNodeValue());
            } else if (subnode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                // Recurse into the subtree for text
                // (and ignore comments)
                result.append(getText(subnode));
            }
        }

        return result.toString();
    }

    public static String toString(final Document document) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            //http://johnsonsolutions.blogspot.ca/2007/08/xml-transformer-indent-doesnt-work-with.html
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(new DOMSource(document), new StreamResult(baos));

            final String result = baos.toString();
            return result;
        } catch (final TransformerException e) {
            throw new Error(e);
        }
    }

    public static Token fromXmlToToken(final Node tokenNode) {
        Token value;

        String tokenValue = null;
        TokenType tokenType = null;
        UUID targetIdentity = Guid.Empty;

        final NodeList propertyNodes = tokenNode.getChildNodes();
        for (int v = 0; v < propertyNodes.getLength(); v++) {
            final Node propertyNode = propertyNodes.item(v);
            final String propertyName = propertyNode.getNodeName();
            if ("Type".equals(propertyName)) {
                tokenType = TokenType.valueOf(TokenType.class, XmlHelper.getText(propertyNode));
            } else if ("Value".equals(propertyName)) {
                tokenValue = XmlHelper.getText(propertyNode);
            } else if ("targetIdentity".equals(propertyName)) {
                targetIdentity = UUID.fromString(XmlHelper.getText(propertyNode));
            }
        }
        value = new Token(tokenValue, tokenType);
        value.setTargetIdentity(targetIdentity);
        return value;
    }

    public static Element toXml(final Document document, final Token token) {
        final Element valueNode = document.createElement("value");

        final Element typeNode = document.createElement("Type");
        final Text typeValue = document.createTextNode(token.Type.toString());
        typeNode.appendChild(typeValue);
        valueNode.appendChild(typeNode);

        final Element tokenValueNode = document.createElement("Value");
        final Text valueValue = document.createTextNode(token.Value);
        tokenValueNode.appendChild(valueValue);
        valueNode.appendChild(tokenValueNode);

        if (!Guid.Empty.equals(token.getTargetIdentity())) {
            final Element targetIdentityNode = document.createElement("targetIdentity");
            final Text targetIdentityValue = document.createTextNode(token.getTargetIdentity().toString());
            targetIdentityNode.appendChild(targetIdentityValue);
            valueNode.appendChild(targetIdentityNode);
        }
        return valueNode;
    }

    public static TokenPair fromXmlToTokenPair(final Node tokenPairNode) {
        TokenPair value;

        String accessToken = null;
        String refreshToken = null;

        final NodeList propertyNodes = tokenPairNode.getChildNodes();
        for (int v = 0; v < propertyNodes.getLength(); v++) {
            final Node propertyNode = propertyNodes.item(v);
            final String propertyName = propertyNode.getNodeName();
            if ("accessToken".equals(propertyName)) {
                accessToken = XmlHelper.getText(propertyNode);
            } else if ("refreshToken".equals(propertyName)) {
                refreshToken = XmlHelper.getText(propertyNode);
            }
        }

        value = new TokenPair(accessToken, refreshToken);
        return value;
    }

    public static Element toXml(final Document document, final TokenPair tokenPair) {
        final Element valueNode = document.createElement("value");

        final Element accessTokenNode = document.createElement("accessToken");
        final Text accessTokenValue = document.createTextNode(tokenPair.AccessToken.Value);
        accessTokenNode.appendChild(accessTokenValue);
        valueNode.appendChild(accessTokenNode);

        final Element refreshTokenNode = document.createElement("refreshToken");
        final Text refreshTokenValue = document.createTextNode(tokenPair.RefreshToken.Value);
        refreshTokenNode.appendChild(refreshTokenValue);
        valueNode.appendChild(refreshTokenNode);

        return valueNode;
    }
}
