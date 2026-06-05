package io.goobi.api.job.actapro.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.interfaces.INodeType;

import de.intranda.goobi.plugins.model.ArchiveManagementConfiguration;
import de.sub.goobi.config.ConfigurationHelper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Getter
@Log4j2
public class ActaProApiConfiguration {

    private List<String> configuredInventories;

    // authentication
    private String authServiceUrl;
    private String authServiceHeader;
    private String authServiceUsername;
    private String authServicePassword;
    private String connectorUrl;

    private String identifierFieldName;

    private String documentOwner;
    private transient List<MetadataMapping> metadataFields;
    private transient Map<String, INodeType> nodes;
    private transient ArchiveManagementConfiguration config;
    private transient XMLConfiguration actaProConfig;

    private boolean enableXmlImport = false;
    private String xmlImportFolder;
    private String xmlTectonicsFile;

    public ActaProApiConfiguration() {
        try {
            config = new ArchiveManagementConfiguration();
            config.readConfiguration("");
        } catch (ConfigurationException e) {
            log.error(e);
        }

        try {
            actaProConfig = new XMLConfiguration(
                    ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_archive_management.xml");
            actaProConfig.setListDelimiter('&');
            actaProConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            actaProConfig.setExpressionEngine(new XPathExpressionEngine());

            // read authentication
            actaProConfig = new XMLConfiguration(
                    ConfigurationHelper.getInstance().getConfigurationFolder() + "plugin_intranda_administration_actapro_sync.xml");
            actaProConfig.setListDelimiter('&');
            actaProConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
            actaProConfig.setExpressionEngine(new XPathExpressionEngine());

            configuredInventories = new ArrayList<>();

            List<HierarchicalConfiguration> hcl = actaProConfig.configurationsAt("/inventory");

            for (HierarchicalConfiguration hc : hcl) {
                configuredInventories.add(hc.getString("@archiveName"));
            }

            authServiceUrl = actaProConfig.getString("/authentication/authServiceUrl");
            authServiceHeader = actaProConfig.getString("/authentication/authServiceHeader");
            authServiceUsername = actaProConfig.getString("/authentication/authServiceUsername");
            authServicePassword = actaProConfig.getString("/authentication/authServicePassword");
            connectorUrl = actaProConfig.getString("/connectorUrl");

            identifierFieldName = actaProConfig.getString("/eadIdField");

            documentOwner = actaProConfig.getString("/documentOwner", "ACTAPRO");

            metadataFields = new ArrayList<>();

            List<HierarchicalConfiguration> mapping = actaProConfig.configurationsAt("/metadata/field");
            for (HierarchicalConfiguration c : mapping) {
                MetadataMapping mm = new MetadataMapping(c.getString("@type"), c.getString("@groupType", ""), c.getString("@eadField"),
                        c.getString("@eadGroup", ""), c.getString("@eadArea"));
                metadataFields.add(mm);
            }

            nodes = new HashMap<>();
            List<HierarchicalConfiguration> nodeTypes = actaProConfig.configurationsAt("/nodeTypes/type");

            INodeType defaultType = null;
            for (INodeType nodeType : config.getConfiguredNodes()) {
                if ("folder".equals(nodeType.getNodeName())) {
                    defaultType = nodeType;
                }
            }

            for (HierarchicalConfiguration c : nodeTypes) {
                String actaProType = c.getString("@actaPro");
                String nodeType = c.getString("@node");
                INodeType type = null;
                for (INodeType nt : config.getConfiguredNodes()) {
                    if (nt.getNodeName().equals(nodeType)) {
                        type = nt;
                    }
                }
                if (type != null) {
                    // use configured type
                    nodes.put(actaProType, type);
                } else {
                    // or default type
                    nodes.put(actaProType, defaultType);
                }
            }
            enableXmlImport = actaProConfig.getBoolean("/xml/@enabled");
            xmlImportFolder = actaProConfig.getString("/xml/importFolder");
            xmlTectonicsFile = actaProConfig.getString("/xml/tectonicsFile");
        } catch (ConfigurationException e2) {
            log.error(e2);
        }

        try {
            ArchiveManagementConfiguration config = new ArchiveManagementConfiguration();
            config.readConfiguration("");
        } catch (ConfigurationException e) {
            log.error(e);
        }
    }

    public String getRootId(String database) {
        return actaProConfig.getString("/inventory[@archiveName='" + database + "']/@actaproId");
    }

}
