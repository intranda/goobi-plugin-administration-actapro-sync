package io.goobi.api.job.actapro.model;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;
import org.goobi.interfaces.IEadEntry;
import org.goobi.interfaces.IMetadataField;
import org.goobi.interfaces.IMetadataGroup;

import de.intranda.goobi.plugins.model.RecordGroup;
import de.sub.goobi.persistence.managers.MySQLHelper;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NodeApi {

    private static final java.util.regex.Pattern VALID_XML_ELEMENT_NAME = java.util.regex.Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_\\-\\.]*$");

    public static void addMetadataValue(IEadEntry entry, MetadataMapping matchedMapping, DocumentField matchedField) {
        String value = matchedField.getPlainValue();
        if (StringUtils.isBlank(value)) {
            value = matchedField.getValue();
        }

        switch (matchedMapping.getEadArea()) {
            case "1":
                for (IMetadataField emf : entry.getIdentityStatementAreaList()) {
                    // add/replace value
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "2":
                for (IMetadataField emf : entry.getContextAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "3":
                for (IMetadataField emf : entry.getContentAndStructureAreaAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "4":
                for (IMetadataField emf : entry.getAccessAndUseAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "5":
                for (IMetadataField emf : entry.getAlliedMaterialsAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "6":
                for (IMetadataField emf : entry.getNotesAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
            case "7":
                for (IMetadataField emf : entry.getDescriptionControlAreaList()) {
                    saveValue(matchedMapping, value, emf);
                }
                break;
        }
    }

    public static String getDocOrder(io.goobi.api.job.actapro.model.Document doc) {
        if (doc == null || doc.getBlock() == null) {
            return null;
        }
        for (DocumentField field : doc.getBlock().getFields()) {
            if ("Ref_Gp".equals(field.getType())) {
                for (DocumentField subfield : field.getFields()) {
                    if ("Ref_DocOrder".equals(subfield.getType())) {
                        return subfield.getValue();
                    }
                }
            }
        }
        return null;
    }

    public static void saveValue(MetadataMapping matchedMapping, String value, IMetadataField emf) {
        if (StringUtils.isNotBlank(matchedMapping.getEadGroup())) {
            if (emf.getName().equals(matchedMapping.getEadGroup())) {
                IMetadataGroup grp = emf.getGroups().get(0);
                for (IMetadataField f : grp.getFields()) {
                    if (f.getName().equals(matchedMapping.getEadField())) {
                        if (!f.getValues().isEmpty()) {
                            f.getValues().get(0).setValue(value);
                        } else {
                            f.addValue();
                            f.getValues().get(0).setValue(value);
                        }
                    }
                }
            }
        } else if (emf.getName().equals(matchedMapping.getEadField())) {
            if (emf.getValues() != null && !emf.getValues().isEmpty()) {
                emf.getValues().get(0).setValue(value);
            } else {
                emf.addValue();
                emf.getValues().get(0).setValue(value);
            }
        }
    }

    public static void saveNode(Integer id, ExtendedEadEntry entry) {

        // get next free id
        if (entry.getDatabaseId() == null) {
            String nextIdSql = "SELECT max(id) +1 from archive_record_node";
            try (Connection connection = MySQLHelper.getInstance().getConnection()) {
                QueryRunner run = new QueryRunner();
                int nextAutoIncrementDbID = run.query(connection, nextIdSql, MySQLHelper.resultSetToIntegerHandler);
                // assign new ids to all entries without id
                entry.setDatabaseId(nextAutoIncrementDbID++);

            } catch (SQLException e) {
                log.error(e);
            }
        }

        String insertSql =
                "INSERT INTO archive_record_node (id, uuid, archive_record_group_id, hierarchy, order_number, node_type, sequence, processtitle, parent_id,label, data) VALUES ";

        StringBuilder values = new StringBuilder();

        if (values.length() > 0) {
            values.append(", ");
        }

        values.append("(");
        values.append(entry.getDatabaseId());
        values.append(", '");
        values.append(entry.getId());
        values.append("', ");
        values.append(id);
        values.append(", ");
        values.append(entry.getHierarchy());
        values.append(", ");
        values.append(entry.getOrderNumber());
        values.append(", '");
        values.append(entry.getNodeType() == null ? "" : entry.getNodeType().getNodeName());
        values.append("', '");
        values.append(entry.getSequence());
        if (entry.getGoobiProcessTitle() == null) {
            values.append("', null, ");
        } else {
            values.append("', '");
            values.append(entry.getGoobiProcessTitle());
            values.append("', ");
        }
        values.append(entry.getParentId());
        values.append(", ?, ? )");
        String metadata = entry.getDataAsXml();

        StringBuilder sql = new StringBuilder(insertSql);
        sql.append(values.toString());
        sql.append("ON DUPLICATE KEY UPDATE  uuid = VALUES(uuid), hierarchy = VALUES(hierarchy), order_number = VALUES(order_number), "
                + "node_type =  VALUES(node_type), sequence = VALUES(sequence), processtitle = VALUES(processtitle), "
                + "processtitle = VALUES(processtitle), parent_id = VALUES(parent_id), label = VALUES(label), data = VALUES(data)");
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            run.update(connection, sql.toString(), entry.getLabel(), metadata);
        } catch (SQLException e) {
            log.error(e);
        }

    }

    public static Map<String, Integer> loadNodeIdCache(RecordGroup recordGroup, String identifierFieldName) {
        Map<String, Integer> cache = new HashMap<>();
        if (!VALID_XML_ELEMENT_NAME.matcher(identifierFieldName).matches()) {
            log.error("Invalid actaProConfig.getIdentifierFieldName() '{}' - rejected to prevent injection", identifierFieldName);
            return cache;
        }
        String sql = "SELECT id, ExtractValue(data, '/xml/" + identifierFieldName + "') AS dockey "
                + "FROM archive_record_node WHERE archive_record_group_id = ?";
        ResultSetHandler<Void> handler = rs -> {
            while (rs.next()) {
                String key = rs.getString("dockey");
                int id = rs.getInt("id");
                if (StringUtils.isNotBlank(key)) {
                    cache.put(key, id);
                }
            }
            return null;
        };
        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            new QueryRunner().query(connection, sql, handler, recordGroup.getId());
        } catch (SQLException e) {
            log.error("Error loading node ID cache: {}", e.getMessage(), e);
        }
        return cache;
    }

    public static ExtendedEadEntry loadExtendendEntry(Integer entryId) {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from archive_record_node WHERE id = ?");

        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, sql.toString(), rresultSetToExtendedNodeHandler, entryId);
        } catch (SQLException e) {
            log.error(e);
        }
        return null;
    }

    public static void clearNode(IEadEntry entry) {
        clearList(entry.getIdentityStatementAreaList());
        clearList(entry.getContextAreaList());
        clearList(entry.getContentAndStructureAreaAreaList());
        clearList(entry.getAccessAndUseAreaList());
        clearList(entry.getAlliedMaterialsAreaList());
        clearList(entry.getNotesAreaList());
        clearList(entry.getDescriptionControlAreaList());
    }

    public static void clearList(List<IMetadataField> list) {
        for (IMetadataField f : list) {
            f.setEadEntry(null);
            f.getValues().clear();
            if (f.isGroup() && f.getGroups() != null) {
                f.getGroups().clear();
            }
        }
        list.clear();
    }

    public static SimpleEadEntry loadSimpleEntry(Integer id) {
        StringBuilder sql = new StringBuilder();
        sql.append("select * from archive_record_node WHERE id = ?");

        try (Connection connection = MySQLHelper.getInstance().getConnection()) {
            QueryRunner run = new QueryRunner();
            return run.query(connection, sql.toString(), resultSetToNodeHandler, id);
        } catch (SQLException e) {
            log.error(e);
        }
        return null;
    }

    private static final ResultSetHandler<ExtendedEadEntry> rresultSetToExtendedNodeHandler = new ResultSetHandler<>() {
        @Override
        public ExtendedEadEntry handle(ResultSet rs) throws SQLException {

            if (rs.next()) {

                int id = rs.getInt("id");
                String uuid = rs.getString("uuid");

                int hierarchy = rs.getInt("hierarchy");
                int orderNumber = rs.getInt("order_number");
                String nodeTypeName = rs.getString("node_type");
                String sequence = rs.getString("sequence");
                String processtitle = rs.getString("processtitle");
                Integer parentId = rs.getInt("parent_id");
                if (rs.wasNull()) {
                    parentId = null;
                }
                String label = rs.getString("label");

                String data = rs.getString("data");

                ExtendedEadEntry currentEntry = new ExtendedEadEntry(orderNumber, hierarchy);
                currentEntry.setParentId(parentId);
                currentEntry.setDatabaseId(id);
                currentEntry.setId(uuid);
                currentEntry.setNodeTypeName(nodeTypeName);

                currentEntry.setSequence(sequence);
                currentEntry.setGoobiProcessTitle(processtitle);
                currentEntry.setLabel(label);
                currentEntry.setData(data);

                return currentEntry;
            }

            return null;
        }
    };

    private static final ResultSetHandler<SimpleEadEntry> resultSetToNodeHandler = new ResultSetHandler<>() {
        @Override
        public SimpleEadEntry handle(ResultSet rs) throws SQLException {
            if (rs.next()) {

                int id = rs.getInt("id");
                String uuid = rs.getString("uuid");

                int hierarchy = rs.getInt("hierarchy");
                int orderNumber = rs.getInt("order_number");
                String sequence = rs.getString("sequence");
                Integer parentId = rs.getInt("parent_id");
                if (rs.wasNull()) {
                    parentId = null;
                }

                SimpleEadEntry currentEntry = new SimpleEadEntry();

                currentEntry.setId(id);
                currentEntry.setUuid(uuid);
                currentEntry.setSequence(sequence);
                currentEntry.setHierarchy(hierarchy);
                currentEntry.setOrder(orderNumber);
                currentEntry.setParentId(parentId);

                currentEntry.setSequence(sequence);

                return currentEntry;

            }
            return null;
        }
    };
}
