/* ********************************************************************
    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.
*/
package org.bedework.timezones.common.h2db;

import org.bedework.timezones.common.TzConfig;
import org.bedework.timezones.common.TzException;
import org.bedework.timezones.common.db.AbstractDb;
import org.bedework.timezones.common.db.TzAlias;
import org.bedework.timezones.common.db.TzDbSpec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/** Cached timezone data in an h2 database.
 *
 * @author douglm
 */
public class H2dbCachedData extends AbstractDb {
  protected ObjectMapper mapper = new ObjectMapper(); // create once, reuse

  /** Current Database
   */
  protected Connection conn;

  private final static String timezoneSpecTable = "TZS";

  private final static String aliasTable = "ALIASES";

  /* Calculated from config db path */
  private String dbPath;

  /** Start from database cache. Fall back is probably to use the
   * zipped data.
   *
   * @param cfg the configuration
   * @param clear remove all data from db first
   * @throws TzException on fatal error
   */
  public H2dbCachedData(final TzConfig cfg,
                        final boolean clear) throws TzException {
    super(cfg, "Db");

    try {
      Class.forName ("org.h2.Driver");

      if (debug()) {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
      }

      final DateFormat df =
              new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'");

      mapper.setDateFormat(df);

      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    } catch (final Throwable t) {
      throw new TzException(t);
    }

    initData(clear);
  }

  /* ====================================================================
   *                   DbCachedData methods
   * ==================================================================== */

  @Override
  public void addTzAlias(final TzAlias val) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "insert into " + aliasTable +
                    " values(?, ?)")) {

      if (debug()) {
        debug("Adding alias: " + val);
      }

      final Blob b = conn.createBlob();
      b.setBytes(0, bytesJson(val));

      stmt.setString(1, val.getAliasId());
      stmt.setBlob(2, b);

      stmt.executeUpdate();
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  @Override
  public void putTzAlias(final TzAlias val) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "update " + aliasTable +
                    " set jsonval=?" +
                    " where id=?")) {
      final Blob b = conn.createBlob();
      b.setBytes(1, bytesJson(val));

      stmt.setBlob(1, b);
      stmt.setString(2, val.getAliasId());

      stmt.executeUpdate();
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  @Override
  public void removeTzAlias(final TzAlias val) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "delete from " + aliasTable +
                    " where id=?")) {
      stmt.setString(1, val.getAliasId());

      stmt.executeUpdate();
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  @Override
  public TzAlias getTzAlias(final String val) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "select jsonval from " + aliasTable +
                    " where id=?")) {
      stmt.setString(1, val);

      try (final ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          return null;
        }

        final TzAlias alias =
                getJson(rs.getBlob("jsonval").getBinaryStream(),
                            TzAlias.class);
        if (rs.next()) {
          throw new TzException("More than one result for fetch alias");
        }

        return alias;
      }
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  private class H2Iterator<T>
          extends DbIterator<T> {
    final String table;
    final Class<T> type;
    ResultSet rs;
    CallableStatement stmt;
    InputStream nextValue;

    H2Iterator(final String table,
               final Class<T> type) {
      this.table = table;
      this.type = type;

      try {
        final Connection conn = getDb();

        stmt = conn.prepareCall(
                "select id, jsonval from " + table);

        rs = stmt.executeQuery();
      } catch (final Throwable t) {
        // Always bad.
        error(t);
        throw new RuntimeException(t);
      }
    }

    @Override
    public void close() throws IOException {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (final SQLException se) {
          throw new IOException(se);
        } finally {
          stmt = null;
        }
      }
    }

    @Override
    public boolean hasNext() {
      try {
        if (nextValue != null) {
          return true;
        }

        if (!rs.next()) {
          return false;
        }

        nextValue = rs.getBlob("jsonval").getBinaryStream();
        return (rs != null) && !rs.isAfterLast();
      } catch (final SQLException se) {
        throw new RuntimeException(se);
      }
    }

    @Override
    public T next() {
      try {
        if (nextValue == null) {
          if (!rs.next()) {
            return null;
          }
          nextValue = rs.getBlob("jsonval")
                        .getBinaryStream();
          if (nextValue == null) {
            return null;
          }
        }

        final T res = getJson(nextValue,
                              type);
        nextValue = null;

        return res;
      } catch (final Throwable t) {
        throw new RuntimeException(t);
      }
    }
  }

  @Override
  protected DbIterator<TzAlias> getAliasIterator() {
    return new H2Iterator<>(aliasTable, TzAlias.class);
  }

  @Override
  protected DbIterator<TzDbSpec> getTzSpecIterator() {
    return new H2Iterator<>(timezoneSpecTable,
                            TzDbSpec.class);
  }

  @Override
  public void addTzSpec(final TzDbSpec val) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "insert into " + timezoneSpecTable +
                    " values(?, ?)")) {

      final Blob b = conn.createBlob();
      b.setBytes(1, bytesJson(val));

      stmt.setString(1, val.getName());
      stmt.setBlob(2, b);

      stmt.executeUpdate();
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  @Override
  public void putTzSpec(final TzDbSpec val) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "update " + timezoneSpecTable +
                    " set jsonval=?" +
                    " where id=?")) {
      final Blob b = conn.createBlob();
      b.setBytes(1, bytesJson(val));
      stmt.setBlob(1, b);
      stmt.setString(2, val.getName());

      stmt.executeUpdate();
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  @Override
  protected TzDbSpec getSpec(final String id) throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.prepareStatement(
            "select jsonval from " + timezoneSpecTable +
                    " where id=?")) {
      stmt.setString(1, id);

      try (final ResultSet rs = stmt.executeQuery()) {
        if (!rs.next()) {
          return null;
        }

        final TzDbSpec spec =
                getJson(rs.getBlob("jsonval").getBinaryStream(),
                        TzDbSpec.class);
        if (rs.next()) {
          throw new TzException("More than one result for fetch tzspec");
        }

        return spec;
      }
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  /* ====================================================================
   *                   Transaction methods
   * ==================================================================== */

  protected void open() throws TzException {
    synchronized (dbLock) {
      if (!isOpen()) {
        getDb();
        open = true;
        return;
      }
    }
  }

  protected void close() {
    synchronized (dbLock) {
      if (open) {
        closeDb();
        open = false;
      }
    }
  }

  @Override
  protected void clearDb() throws TzException {
    final Connection conn = getDb();

    try (final var stmt = conn.createStatement()) {
      stmt.executeUpdate("DROP TABLE IF EXISTS " + timezoneSpecTable);
      stmt.executeUpdate("DROP TABLE IF EXISTS " + aliasTable);

      stmt.executeUpdate("CREATE TABLE " + timezoneSpecTable +
              "(id VARCHAR(255), " +
              " jsonval BLOB, " +
              " PRIMARY KEY (id))");

      stmt.executeUpdate("CREATE TABLE " + aliasTable +
              "(id VARCHAR(255), " +
              " jsonval BLOB, " +
              " PRIMARY KEY (id))");
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }
  }

  /* ====================================================================
   *                   private methods
   * ==================================================================== */

  private Connection getDb() throws TzException {
    if (conn != null) {
      return conn;
    }

    try {
      dbPath = cfg.getDbPath();

      if (debug()) {
        debug("Try to open db at " + dbPath);
      }

      conn = DriverManager.getConnection("jdbc:h2:" + dbPath,
                                         "sa", "");

      final ResultSet rset =
              conn.getMetaData().getTables(null, null,
                                           aliasTable, null);
      if (!rset.next()) {
        clearDb();
        loadInitialData();
      }
    } catch (final Throwable t) {
      // Always bad.
      error(t);
      throw new TzException(t);
    }

    return conn;
  }

  private void closeDb() {
    if (conn == null) {
      return;
    }

    try {
      conn.close();
      conn = null;
    } catch (final Throwable t) {
      warn("Error closing db: " + t.getMessage());
      error(t);
    }
  }

  /** ===================================================================
   *                   Json methods
   *  =================================================================== */

  protected byte[] bytesJson(final Object val) throws TzException {
    try {
      final ByteArrayOutputStream os = new ByteArrayOutputStream();

      mapper.writeValue(os, val);

      return os.toByteArray();
    } catch (final Throwable t) {
      throw new TzException(t);
    }
  }

  protected <T> T getJson(final InputStream is,
                          final Class<T> valueType) throws TzException {
    try {
      return mapper.readValue(is, valueType);
    } catch (final Throwable t) {
      warn("Unable to parse json value");
      throw new TzException(t);
    }
  }
}
