// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.jwt;

import static com.google.common.truth.Truth.assertThat;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for RawJwt */
@RunWith(JUnit4.class)
public final class RawJwtTest {

  @Test
  public void noIssuer_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getClaim(JwtNames.CLAIM_ISSUER)).isNull();
  }

  @Test
  public void setIssuer_success() throws Exception {
    RawJwt token = new RawJwt.Builder().setIssuer("foo").build();

    assertThat(token.getClaim(JwtNames.CLAIM_ISSUER)).isEqualTo("foo");
  }

  @Test
  public void noAudience_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getClaim(JwtNames.CLAIM_AUDIENCE)).isNull();
  }

  @Test
  public void addAudience_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addAudience("foo").build();
    JSONArray audiences = (JSONArray) token.getClaim(JwtNames.CLAIM_AUDIENCE);

    assertThat(audiences.getString(0)).isEqualTo("foo");
  }

  @Test
  public void addMulitpleAudiences_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addAudience("foo").addAudience("bar").build();
    JSONArray audiences = (JSONArray) token.getClaim(JwtNames.CLAIM_AUDIENCE);

    assertThat(audiences.getString(0)).isEqualTo("foo");
    assertThat(audiences.getString(1)).isEqualTo("bar");
  }

  @Test
  public void noSubject_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getClaim(JwtNames.CLAIM_SUBJECT)).isNull();
  }

  @Test
  public void setSubject_success() throws Exception {
    RawJwt token = new RawJwt.Builder().setSubject("foo").build();

    assertThat(token.getClaim(JwtNames.CLAIM_SUBJECT)).isEqualTo("foo");
  }

  @Test
  public void noJwtId_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getClaim(JwtNames.CLAIM_JWT_ID)).isNull();
  }

  @Test
  public void setJwtId_success() throws Exception {
    RawJwt token = new RawJwt.Builder().setJwtId("foo").build();

    assertThat(token.getClaim(JwtNames.CLAIM_JWT_ID)).isEqualTo("foo");
  }

  @Test
  public void addClaim_issuer_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_ISSUER, "blah"));
  }

  @Test
  public void addClaim_subject_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_SUBJECT, "blah"));
  }

  @Test
  public void addClaim_audience_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_AUDIENCE, "blah"));
  }

  @Test
  public void addClaim_jwtId_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_JWT_ID, "blah"));
  }

  @Test
  public void addClaim_expiration_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_EXPIRATION, Instant.now()));
  }

  @Test
  public void addClaim_issuedAt_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_ISSUED_AT, Instant.now()));
  }

  @Test
  public void addClaim_notBefore_shouldThrow() throws Exception {
    assertThrows(
        IllegalArgumentException.class,
        () -> new RawJwt.Builder().addClaim(JwtNames.CLAIM_NOT_BEFORE, Instant.now()));
  }

  @Test
  public void addClaim_string_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addClaim("claim", "value").build();

    assertThat(token.getClaim("claim")).isEqualTo("value");
  }

  @Test
  public void addClaim_integer_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addClaim("claim", 1).build();

    assertThat(token.getClaim("claim")).isEqualTo(1);
  }

  @Test
  public void addClaim_long_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addClaim("claim", 1L).build();

    assertThat(token.getClaim("claim")).isEqualTo(1L);
  }

  @Test
  public void addClaim_double_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addClaim("claim", 123.4).build();

    assertThat(token.getClaim("claim")).isEqualTo(123.4);
  }

  @Test
  public void addClaim_boolean_success() throws Exception {
    RawJwt token = new RawJwt.Builder().addClaim("claim", true).build();

    assertThat(token.getClaim("claim")).isEqualTo(true);
  }

  @Test
  public void addClaim_none_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getClaim("claim")).isNull();
  }

  @Test
  public void noExpiration_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getExpiration()).isNull();
  }

  @Test
  public void setExpiration_success() throws Exception {
    Instant instant = Instant.now();
    RawJwt token = new RawJwt.Builder().setExpiration(instant).build();

    assertThat(token.getExpiration()).isEqualTo(instant.truncatedTo(SECONDS));
  }

  @Test
  public void noNotBefore_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getNotBefore()).isNull();
  }

  @Test
  public void setNotBefore_success() throws Exception {
    Instant instant = Instant.now();
    RawJwt token = new RawJwt.Builder().setNotBefore(instant).build();

    assertThat(token.getNotBefore()).isEqualTo(instant.truncatedTo(SECONDS));
  }

  @Test
  public void noIssuedAt_success() throws Exception {
    RawJwt token = new RawJwt.Builder().build();

    assertThat(token.getIssuedAt()).isNull();
  }

  @Test
  public void setIssuedAt_success() throws Exception {
    Instant instant = Instant.now();
    RawJwt token = new RawJwt.Builder().setIssuedAt(instant).build();

    assertThat(token.getIssuedAt()).isEqualTo(instant.truncatedTo(SECONDS));
  }

  @Test
  public void getPayload_success() throws Exception {
    RawJwt token = new RawJwt.Builder().setJwtId("blah").build();
    JSONObject playload = token.getPayload();
    assertThat(playload.get(JwtNames.CLAIM_JWT_ID)).isEqualTo("blah");
  }
}
