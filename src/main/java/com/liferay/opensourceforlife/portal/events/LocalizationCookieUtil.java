/**
 * 
 */
package com.liferay.opensourceforlife.portal.events;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Hex;

import com.liferay.portal.CookieNotSupportedException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CookieKeys;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.util.CookieUtil;

/**
 * @author Tejas Kanani
 */
public class LocalizationCookieUtil
{

	public static final int MAX_AGE = 31536000;
	public static final int VERSION = 0;

	/**
	 * @param request
	 * @throws CookieNotSupportedException
	 */
	public static void validateSupportCookie(final HttpServletRequest request)
			throws CookieNotSupportedException
	{
		if (GetterUtil.getBoolean(PropsUtil.get(PropsKeys.SESSION_ENABLE_PERSISTENT_COOKIES))
				&& GetterUtil.getBoolean(PropsUtil.get(PropsKeys.SESSION_TEST_COOKIE_SUPPORT)))
		{
			String cookieSupport = getCookie(request, CookieKeys.COOKIE_SUPPORT, false);

			if (Validator.isNull(cookieSupport))
			{
				throw new CookieNotSupportedException();
			}
		}
	}

	/**
	 * @param request
	 * @param name
	 * @param toUpperCase
	 * @return
	 */
	private static String getCookie(final HttpServletRequest request, final String name,
			final boolean toUpperCase)
	{
		String value = CookieUtil.get(request, name, toUpperCase);

		if (value != null && isEncodedCookie(name))
		{
			try
			{
				String encodedValue = value;
				String originalValue = new String(Hex.decodeHex(encodedValue.toCharArray()));

				if (_log.isDebugEnabled())
				{
					_log.debug("Get encoded cookie " + name);
					_log.debug("Hex encoded value " + encodedValue);
					_log.debug("Original value " + originalValue);
				}

				return originalValue;
			} catch (Exception e)
			{
				if (_log.isWarnEnabled())
				{
					_log.warn(e.getMessage());
				}

				return value;
			}
		}

		return value;
	}

	/**
	 * @param name
	 * @return
	 */
	private static boolean isEncodedCookie(final String name)
	{
		if (name.equals(CookieKeys.ID) || name.equals(CookieKeys.LOGIN)
				|| name.equals(CookieKeys.PASSWORD) || name.equals(CookieKeys.SCREEN_NAME))
		{
			return true;
		} else
		{
			return false;
		}
	}

	/**
	 * @param request
	 * @param response
	 */
	public static void addSupportCookie(final HttpServletRequest request,
			final HttpServletResponse response)
	{

		Cookie cookieSupportCookie = new Cookie(CookieKeys.COOKIE_SUPPORT, "true");

		cookieSupportCookie.setPath(StringPool.SLASH);
		cookieSupportCookie.setMaxAge(MAX_AGE);

		addCookie(request, response, cookieSupportCookie);
	}

	/**
	 * @param request
	 * @param response
	 * @param cookie
	 */
	private static void addCookie(final HttpServletRequest request,
			final HttpServletResponse response, final Cookie cookie)
	{

		addCookie(request, response, cookie, request.isSecure());
	}

	/**
	 * @param request
	 * @param response
	 * @param cookie
	 * @param secure
	 */
	private static void addCookie(final HttpServletRequest request,
			final HttpServletResponse response, final Cookie cookie, final boolean secure)
	{
		if (!GetterUtil.getBoolean(PropsUtil.get(PropsKeys.SESSION_ENABLE_PERSISTENT_COOKIES))
				|| GetterUtil.getBoolean(PropsUtil.get(PropsKeys.TCK_URL)))
		{
			return;
		}

		// LEP-5175
		String name = cookie.getName();

		String originalValue = cookie.getValue();
		String encodedValue = originalValue;

		if (isEncodedCookie(name))
		{
			encodedValue = new String(Hex.encodeHex(originalValue.getBytes()));

			if (_log.isDebugEnabled())
			{
				_log.debug("Add encoded cookie " + name);
				_log.debug("Original value " + originalValue);
				_log.debug("Hex encoded value " + encodedValue);
			}
		}

		cookie.setSecure(secure);
		cookie.setValue(encodedValue);
		cookie.setVersion(VERSION);

		// Setting a cookie will cause the TCK to lose its ability to track
		// sessions

		response.addCookie(cookie);
	}

	private static Log _log = LogFactoryUtil.getLog(LocalizationCookieUtil.class);
}
