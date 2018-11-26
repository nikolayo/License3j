package com.javax0.license3j.licensor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * ExtendedLicense supports not only String features, but also Integer, Date and
 * URL features. It is also able to check that the license is not expired and
 * can check the revocation state of the license online.
 *
 * @author Peter Verhas
 */
public class ExtendedLicense extends License {

    final private static String EXPIRATION_DATE = "expiryDate";
    final private static String DATE_FORMAT = "yyyy-MM-dd";
    final private static String LICENSE_ID = "licenseId";
    final private static String REVOCATION_URL = "revocationUrl";
    HttpHandler httpHandler = new HttpHandler();

    /**
     * Checks the expiration date of the license and returns true if the license
     * has expired.
     * <p>
     * The expiration date is encoded in the license with the key
     * {@code expiryDate} in the format {@code yyyy-MM-dd}. A license is expired
     * if the current date is after the specified expiryDate. At the given date
     * the license is still valid.
     * <p>
     * Note that this method does not ensure license validity. You separately
     * have to call {@link License#isVerified()} to ensure that the license was
     * successfully verified.
     * <p>
     * The time is calculated using the default locale, thus licenses expire
     * first in Australia, later in Europe and latest in USA.
     *
     * @return {@code true} if the license is expired
     */
    public boolean isExpired() {
        boolean expired;
        try {
            final Date expiryDate = getFeature(EXPIRATION_DATE, Date.class);
            final GregorianCalendar today = new GregorianCalendar();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            expired = today.getTime().after(expiryDate);
        } catch (final Exception e) {
            expired = true;
        }
        return expired;
    }

    /**
     * Set the expiration date of the license. Since the date is stored in the
     * format {@code yyyy-MM-dd} the actual hours, minutes and so on will be
     * chopped off.
     *
     * @param expiryDate the date when the license expires
     */
    public void setExpiry(final Date expiryDate) {
        setFeature(EXPIRATION_DATE, expiryDate);
    }

    /**
     * Generates a new license id.
     * <p>
     * Note that this ID is also stored in the license thus there is no need to
     * call {@link #setFeature(String, UUID)} separately after the UUID was
     * generated.
     * <p>
     * Generating UUID can be handy when you want to identify each license
     * individually. For example you want to store revocation information about
     * each license. The url to check the revocation may contain the
     * {@code $&#123;licenseId&#125;} place holder that will be replaced by the actual
     * uuid stored in the license.
     *
     * @return the generated uuid.
     */
    public UUID generateLicenseId() {
        final UUID uuid = UUID.randomUUID();
        setLicenseId(uuid);
        return uuid;
    }

    public UUID getLicenseId() {
        UUID licenseId;
        try {
            licenseId = getFeature(LICENSE_ID, UUID.class);
        } catch (final Exception e) {
            licenseId = null;
        }
        return licenseId;
    }

    /**
     * Set the UUID of a license. Note that this UUID can be generated calling
     * the method {@link #generateLicenseId()}, which method automatically calls
     * this method setting the generated UUID to be the UUID of the license.
     *
     * @param licenseId the uuid that was generated somewhere, presumably not by
     *                  {@link #generateLicenseId()} because the uuid generated by
     *                  that method is automatically stored in the license.
     */
    public void setLicenseId(final UUID licenseId) {
        setFeature(LICENSE_ID, licenseId);
    }

    /**
     * Set an integer feature in the license.
     *
     * @param name the name of the feature
     * @param i    the value of the integer feature
     */
    public void setFeature(final String name, final Integer i) {
        setFeature(name, i.toString());
    }

    /**
     * Set a date feature in the license.
     *
     * @param name the name of the feature
     * @param date the date to be stored for the feature name in the license
     */
    public void setFeature(final String name, final Date date) {
        final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        setFeature(name, formatter.format(date));
    }

    /**
     * Set a URL feature in the license.
     *
     * @param name the name of the feature
     * @param url  the url to store in the license
     */
    public void setFeature(final String name, final URL url) {
        setFeature(name, url.toString());
    }

    /**
     * Set an UUID feature in the license.
     *
     * @param name the name of the feature
     * @param uuid the uuid to store in the license
     */
    public void setFeature(final String name, final UUID uuid) {
        setFeature(name, uuid.toString());
    }

    /**
     * Get the value of a feature in the given class format.
     *
     * @param name  identifying the feature
     * @param klass can be Integer.class, Date.class, UUID.class or URL.class
     * @param <T>   the class template type
     * @return the parsed value as an instance of the second argument 'klass'
     */
    public <T> T getFeature(final String name, final Class<? extends T> klass) {
        final T result;
        final String resultString = getFeature(name);
        try {
            if (Integer.class == klass) {
                result = (T) (Integer) Integer.parseInt(resultString);
            } else if (Date.class == klass) {
                result = (T) new SimpleDateFormat(DATE_FORMAT)
                        .parse(getFeature(name));
            } else if (UUID.class == klass) {
                result = (T) UUID.fromString(getFeature(name));
            } else if (URL.class == klass) {
                result = (T) new URL(getFeature(name));
            } else {
                throw new IllegalArgumentException("'" + klass.toString()
                        + "' is not handled");
            }
        } catch (ParseException |
                MalformedURLException |
                IllegalArgumentException shouldNotHappen) {
            throw new IllegalArgumentException(shouldNotHappen);
        }
        return result;
    }

    /**
     * Get the revocation URL of the license. This feature is stored in the
     * license under the name {@code revocationUrl}. This URL may contain the
     * string <code>${licenseId}</code> which is replaced by the actual license
     * ID. Thus there is no need to wire into the revocation URL the license ID.
     * <p>
     * If there is no license id defined in the license then the place holder
     * will not be replaced.
     * <p>
     * Later versions will be extended to allow <code>${featureName}</code>
     * placeholders for any feature.
     *
     * @return the revocation URL with the license id place holder filled in.
     * @throws MalformedURLException when the revocation url is not well formatted
     */
    public URL getRevocationURL() throws MalformedURLException {
        URL url = null;
        final String revocationURLTemplate = getFeature(REVOCATION_URL);
        final String revocationURL;
        if (revocationURLTemplate != null) {
            final UUID licenseId = getLicenseId();
            if (licenseId != null) {
                revocationURL = revocationURLTemplate.replaceAll(
                        "\\$\\{licenseId}", licenseId.toString());
            } else {
                revocationURL = revocationURLTemplate;
            }
            url = new URL(revocationURL);
        }
        return url;
    }

    /**
     * Set the revocation URL. This method accepts the url as a string that
     * makes it possible to use a string that contains the
     * <code>${licenseId}</code> place holder.
     *
     * @param url the url from where the revocation information can be
     *            downloaded
     */
    public void setRevocationURL(final String url) {
        setFeature(REVOCATION_URL, url);
    }

    /**
     * Set the revocation URL. Using this method is discouraged in case the URL
     * contains the <code>${licenseId}</code> place holder. In that case it is
     * recommended to use the {@link #setRevocationURL(String)} method instead.
     *
     * @param url the revocation url
     */
    public void setRevocationURL(final URL url) {
        setRevocationURL(url.toString());
    }

    /**
     * Check if the license was revoked or not. For more information see the
     * documentation of the method {@link #isRevoked(boolean)}. Calling this
     * method is equivalent to calling {@code isRevoked(false)}, meaning that
     * the license is signaled not revoked if the revocation URL can not be
     * reached.
     *
     * @return {@code true} if the license was revoked and {@code false} if the
     * license was not revoked. It also returns {@code true} if the
     * revocation url is unreachable.
     */
    public boolean isRevoked() {
        return isRevoked(false);
    }

    /**
     * Check if the license is revoked or not. To get the revocation information
     * the method tries to issue a http connection (GET) to the url specified in
     * the license feature {@code revocationUrl}. If the URL returns anything
     * with http status code {@code 200} then the license is not revoked.
     * <p>
     * The url string in the feature {@code revocationUrl} may contain the place
     * holder <code>${licenseId}</code>, which is replaced by the feature value
     * {@code licenseId}. This feature makes it possible to setup a revocation
     * service and use a constant string in the different licenses.
     * <p>
     * The method can work in two different ways. One way is to ensure that the
     * license is not revoked and return {@code true} only if it is sure that
     * the license is revoked or revocation information is not available.
     * <p>
     * The other way is to ensure that the license is revoked and return
     * {@code false} if the license was not revoked or the revocation
     * information is not available.
     * <p>
     * The difference is whether to treat the license revoked when the
     * revocation service is not reachable.
     *
     * @param defaultRevocationState should be {@code true} to treat the license revoked when the
     *                               revocation service is not reachable. Setting this argument
     *                               {@code false} makes the revocation handling more polite: if
     *                               the license revocation service is not reachable then the
     *                               license is treated as not revoked.
     * @return {@code true} if the license is revoked and {@code false} if the
     * license is not revoked.
     */
    public boolean isRevoked(final boolean defaultRevocationState) {
        boolean revoked = true;
        try {
            final URL url = getRevocationURL();
            if (url != null) {
                final URLConnection connection = httpHandler.openConnection(url);
                doNotUseCache(connection);
                if (connection instanceof HttpURLConnection) {
                    final HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
                    httpUrlConnection.connect();
                    final int responseCode = httpHandler.getResponseCode(httpUrlConnection);
                    revoked = responseCode != 200;
                }
            } else {
                revoked = false;
            }
        } catch (final IOException exception) {
            revoked = defaultRevocationState;
        }
        return revoked;
    }

    private void doNotUseCache(URLConnection connection) {
        connection.setUseCaches(false);
    }

}
