package com.example.mapboxdemo2

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class MyPageFragment : Fragment(R.layout.fragment_mypage) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- レイアウト(XML)からUI要素を取得 ---
        val userCategoryTextView = view.findViewById<TextView>(R.id.userCategoryTextView)
        val nicknameTextView = view.findViewById<TextView>(R.id.nicknameTextView)
        val ticketCountTextView = view.findViewById<TextView>(R.id.ticketCountTextView)
        val loginRegisterButton = view.findViewById<Button>(R.id.button_login_register)
        val helpMenu = view.findViewById<TextView>(R.id.helpMenu)
        val tutorialMenu = view.findViewById<TextView>(R.id.tutorialMenu)
        val editProfileButton = view.findViewById<ImageButton>(R.id.editProfileButton)
        val termsOfServiceMenu = view.findViewById<TextView>(R.id.termsOfServiceMenu)
        val privacyPolicyMenu = view.findViewById<TextView>(R.id.privacyPolicyMenu)


        // --- 今後、ログイン状態に応じてUIを切り替える ---
        val isLoggedIn = false // TODO: 本来はログイン状態を判定する
        if (isLoggedIn) {
            // ログイン時の表示
            userCategoryTextView.text = "登録ユーザー" // 例
            nicknameTextView.text = "あなたのニックネーム" // 例
            loginRegisterButton.text = "ログアウト"
            editProfileButton.visibility = View.VISIBLE // 編集ボタンを表示
        } else {
            // ログアウト時の表示
            userCategoryTextView.text = "未登録ユーザー"
            nicknameTextView.text = "ゲストさん"
            editProfileButton.visibility = View.GONE // 編集ボタンを非表示
        }
        // チケット数はログイン状態に関わらず表示すると仮定
        ticketCountTextView.text = "保有チケット: 0枚"


        // --- 各ボタンが押された時の「仮の」動作を設定 ---

        editProfileButton.setOnClickListener {
            Toast.makeText(requireContext(), "プロフィール編集がタップされました", Toast.LENGTH_SHORT).show()
            // TODO: プロフィール編集画面への遷移処理を実装
        }

        loginRegisterButton.setOnClickListener {
            if (isLoggedIn) {
                Toast.makeText(requireContext(), "ログアウトします", Toast.LENGTH_SHORT).show()
                // TODO: ログアウト処理
            } else {
                Toast.makeText(requireContext(), "登録/ログイン画面へ", Toast.LENGTH_SHORT).show()
                // TODO: ログイン・登録画面への遷移処理を実装
            }
        }

        helpMenu.setOnClickListener {
            Toast.makeText(requireContext(), "ヘルプ・使い方ガイドがタップされました", Toast.LENGTH_SHORT).show()
            // TODO: ヘルプ画面への遷移、またはダイアログ表示を実装
        }

        tutorialMenu.setOnClickListener {
            Toast.makeText(requireContext(), "チュートリアルを再表示がタップされました", Toast.LENGTH_SHORT).show()
            // TODO: チュートリアルを再表示する処理を実装
        }

        // ★追加したリスナー
        termsOfServiceMenu.setOnClickListener {
            Toast.makeText(requireContext(), "利用規約がタップされました", Toast.LENGTH_SHORT).show()
            // TODO: 利用規約画面へ遷移、またはWebViewで表示
        }

        privacyPolicyMenu.setOnClickListener {
            Toast.makeText(requireContext(), "プライバシーポリシーがタップされました", Toast.LENGTH_SHORT).show()
            // TODO: プライバシーポリシー画面へ遷移、またはWebViewで表示
        }
    }
}