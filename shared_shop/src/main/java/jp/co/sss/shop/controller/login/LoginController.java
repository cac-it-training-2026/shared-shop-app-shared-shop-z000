package jp.co.sss.shop.controller.login;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jp.co.sss.shop.bean.UserBean;
import jp.co.sss.shop.entity.User;
import jp.co.sss.shop.form.LoginForm;
import jp.co.sss.shop.repository.UserRepository;
import jp.co.sss.shop.util.Constant;

/**
 * ログイン機能のコントローラクラス
 *
 * @author SystemShared
 */
@Controller
public class LoginController {

	/**
	 * 会員情報
	 */
	@Autowired
	UserRepository userRepository;

	/**
	 * セッション情報
	 */
	@Autowired
	HttpSession session;

	/**
	 * メッセージソース
	 */
	@Autowired
	MessageSource messageSource;

	/**
	 * ログイン処理
	 *
	 * @param form ログインフォーム
	 * @return "login" ログイン画面表示
	 */
	@RequestMapping(path = "/login", method = RequestMethod.GET)
	public String login(@ModelAttribute LoginForm form) {

		// セッション情報を無効にする
		session.invalidate();

		return "login";
	}

	/**
	 * ログイン処理
	 *
	 * @param form ログインフォーム
	 * @param result 入力チェック結果
	 * @return
			一般会員の場合 "redirect:/" トップ画面表示処理
			運用管理者、システム管理者の場合 "redirect:/adminmenu"管理者メニュー表示処理
	 */
	@RequestMapping(path = "/login", method = RequestMethod.POST)
	public String doLogin(@Valid @ModelAttribute LoginForm form, BindingResult result) {

		String returnStr = "login";
		if (result.hasErrors()) {
			// 入力値に誤りがあった場合
			// セッション情報を無効にして、ログイン画面再表示
			session.invalidate();
			returnStr = "login";

		} else {
			User user = userRepository.findByEmailAndDeleteFlag(form.getEmail(), Constant.NOT_DELETED);

			if (user != null) {
				if (user.getAccountLockedUntil() != null && LocalDateTime.now().isBefore(user.getAccountLockedUntil())) {
					// アカウントがロックされている場合
					result.addError(new FieldError(result.getObjectName(), "email",
							messageSource.getMessage("msg.login.account.locked", null, null)));
					session.invalidate();
					return returnStr;
				}

				if (form.getPassword().equals(user.getPassword())) {
					// ログイン成功
					user.setLoginFailureCount(0);
					user.setAccountLockedUntil(null);
					userRepository.save(user);

					UserBean userBean = new UserBean();
					userBean.setId(user.getId());
					userBean.setName(user.getName());
					userBean.setAuthority(user.getAuthority());
					session.setAttribute("user", userBean);

					// セッションスコープから権限を取り出す
					Integer authority = ((UserBean) session.getAttribute("user")).getAuthority();
					if (authority.intValue() == Constant.AUTH_CLIENT) {
						// 一般会員ログインした場合、トップ画面表示処理にリダイレクト
						returnStr = "redirect:/";
					} else {
						// 運用管理者、もしくはシステム管理者としてログインした場合、管理者用メニュー画面表示処理にリダイレクト
						returnStr = "redirect:/admin/menu";
					}
				} else {
					// パスワードが間違っている場合
					int failCount = user.getLoginFailureCount() + 1;
					user.setLoginFailureCount(failCount);

					if (failCount >= 5) {
						user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
					}
					userRepository.save(user);

					result.addError(new FieldError(result.getObjectName(), "password",
							messageSource.getMessage("msg.login.email.or.password.mismatch", null, null)));
					session.invalidate();
				}
			} else {
				// ユーザーが存在しない場合
				result.addError(new FieldError(result.getObjectName(), "email",
						messageSource.getMessage("msg.login.email.or.password.mismatch", null, null)));
				session.invalidate();
			}
		}
		return returnStr;

	}

	/**
	 * 管理者メニュー表示処理
	 *
	 * @return "admin/menu" 管理者メニュー画面表示
	 */
	@RequestMapping(path = "/admin/menu", method = RequestMethod.GET)
	public String showAdminMenu() {

		// 管理者用メニュー画面表示
		return "admin/admin_menu";
	}

}
