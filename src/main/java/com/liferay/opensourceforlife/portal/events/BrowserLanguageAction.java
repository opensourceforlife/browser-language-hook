/**
 * 
 */
package com.liferay.opensourceforlife.portal.events;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.liferay.portal.kernel.events.Action;
import com.liferay.portal.kernel.events.ActionException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.User;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;

/**
 * It will change logged user's preferred language with the one provided by browser as Accept
 * Language header. So the language selected in browser will become logged in user's preferred
 * language after login.
 * 
 * @author Tejas Kanani
 */
public class BrowserLanguageAction extends Action
{

	public static final String I18N_LANGUAGE_ID = "I18N_LANGUAGE_ID";
	public static final String LOCALE_KEY = "org.apache.struts.action.LOCALE";

	/*
	 * (non-Javadoc)
	 * @see com.liferay.portal.kernel.events.Action#run(javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void run(final HttpServletRequest request, final HttpServletResponse response)
			throws ActionException
	{
		/*
		 * First check for locale.default.request property value. If it's found true in
		 * portal-ext.properties then only go for setting browser's language as logged in user's
		 * language
		 */
		if (GetterUtil.getBoolean(PropsUtil.get(PropsKeys.LOCALE_DEFAULT_REQUEST)))
		{
			doRun(request, response);
		}
	}

	/**
	 * @param request
	 * @param response
	 */
	private void doRun(final HttpServletRequest request, final HttpServletResponse response)
	{
		// Get locale from current request
		Locale locale = getLocaleFromRequest(request);

		if (Validator.isNotNull(locale))
		{
			// Get available locales currently supported/provided in Liferay
			List<Locale> availableLocales = ListUtil.fromArray(LanguageUtil.getAvailableLocales());
			if (availableLocales.contains(locale))
			{
				boolean userLanguageChanged = false;
				try
				{
					// Change current logged in user's preferred language
					User user = PortalUtil.getUser(request);
					user.setLanguageId(LocaleUtil.toLanguageId(locale));
					UserLocalServiceUtil.updateUser(user);

					userLanguageChanged = true;
				} catch (SystemException e)
				{
					LOG.error(e.getMessage(), e);
				} catch (PortalException e)
				{
					LOG.error(e.getMessage(), e);
				}

				if (userLanguageChanged)
				{
					/*
					 * Set current locale in "org.apache.struts.action.LOCALE" session attribute and
					 * also modify the "GUEST_LANGUAGE_ID" cookie for further use
					 */
					setLocale(request, response, locale);

					try
					{
						// LEP-4069
						LocalizationCookieUtil.validateSupportCookie(request);
					} catch (Exception e)
					{
						LocalizationCookieUtil.addSupportCookie(request, response);
					}
				}
			}
		}
	}

	/**
	 * @param request
	 * @param response
	 * @param locale
	 */
	private void setLocale(final HttpServletRequest request, final HttpServletResponse response,
			final Locale locale)
	{
		HttpSession session = request.getSession();
		session.setAttribute(LOCALE_KEY, locale);
		LanguageUtil.updateCookie(request, response, locale);
	}

	/**
	 * @param request
	 * @param locale
	 */
	private Locale getLocaleFromRequest(final HttpServletRequest request)
	{
		Locale locale = null;

		@SuppressWarnings("unchecked")
		Enumeration<Locale> locales = request.getLocales();
		while (locales.hasMoreElements())
		{
			locale = getAvailableLocale(locales.nextElement());
			if (Validator.isNotNull(locale))
			{
				break;
			}
		}

		return locale;
	}

	/**
	 * @param locale
	 * @return
	 */
	private Locale getAvailableLocale(Locale locale)
	{
		if (Validator.isNull(locale.getCountry()))
		{
			// Locales must contain a country code
			locale = LanguageUtil.getLocale(locale.getLanguage());
		}

		if (!LanguageUtil.isAvailableLocale(locale))
		{
			return null;
		}

		return locale;
	}

	private static Log LOG = LogFactoryUtil.getLog(BrowserLanguageAction.class);

}
