package com.weighttracker.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.weighttracker.entity.WeightRecord;
import com.weighttracker.service.WeatherService;
import com.weighttracker.service.WeightService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/")  // ベースURLを明示的に指定
public class WeightController {
    
    @Autowired
    private WeightService weightService;
    
    @Autowired
    private WeatherService weatherService;
    
    // ホームページを表示
    @GetMapping
    public String home(Model model) {
        // 仮のユーザーID
        Integer userId = 1;
        
        // ユーザーの体重記録を取得
        List<WeightRecord> records = weightService.getWeightRecordsByUserId(userId);
        model.addAttribute("weightRecords", records);
        
        // 平均体重を計算
        Double averageWeight = weightService.calculateAverageWeight(userId);
        model.addAttribute("averageWeight", averageWeight);
        
        // 最新の体重記録がある場合、ランクを評価
        if (!records.isEmpty()) {
            Double latestWeight = records.get(0).getWeight();
            String rank = weightService.evaluateWeight(latestWeight, averageWeight);
            model.addAttribute("latestWeight", latestWeight);
            model.addAttribute("rank", rank);
        }
        
        // 天気情報を取得して追加
        Map<String, Object> weatherData = weatherService.getDefaultCityWeather();
        model.addAttribute("weatherData", weatherData);
        
        // 天気情報に基づく健康アドバイスを追加
        String healthAdvice = weatherService.generateHealthAdvice(weatherData);
        model.addAttribute("healthAdvice", healthAdvice);
        
        return "index";
    }
    
    // グラフページを表示
    @GetMapping("/chart")
    public String showChart(Model model) {
        // 仮のユーザーID
        Integer userId = 1;
        
        // ユーザーの体重記録を取得
        List<WeightRecord> records = weightService.getWeightRecordsByUserId(userId);
        model.addAttribute("weightRecords", records);
        
        // 平均体重を計算
        Double averageWeight = weightService.calculateAverageWeight(userId);
        model.addAttribute("averageWeight", averageWeight);
        
        return "chart";
    }
    
    // 履歴ページを表示
    @GetMapping("/history")
    public String showHistory(Model model) {
        // 仮のユーザーID
        Integer userId = 1;
        
        // ユーザーの体重記録を取得
        List<WeightRecord> records = weightService.getWeightRecordsByUserId(userId);
        model.addAttribute("weightRecords", records);
        
        // 平均体重を計算
        Double averageWeight = weightService.calculateAverageWeight(userId);
        model.addAttribute("averageWeight", averageWeight);
        
        return "history";
    }
    
    // 設定ページを表示
    @GetMapping("/settings")
    public String showSettings(Model model) {
        return "settings";
    }
    
    // 詳細ページを表示
    @GetMapping("/details/{id}")
    public String showDetails(@PathVariable("id") Long id, Model model) {
        // 体重記録の詳細を取得
        WeightRecord record = weightService.getWeightRecordById(id);
        model.addAttribute("weightRecord", record);
        
        // 仮のユーザーID
        Integer userId = 1;
        
        // 平均体重を計算
        Double averageWeight = weightService.calculateAverageWeight(userId);
        model.addAttribute("averageWeight", averageWeight);
        
        // ランクを評価
        String rank = weightService.evaluateWeight(record.getWeight(), averageWeight);
        model.addAttribute("rank", rank);
        
        return "details";
    }
    
    @PostMapping("/add")
    public String addWeight(@RequestParam("weight") Double weight, 
                        @RequestParam("recordedDate") String recordedDateStr) {
        // デバッグ出力を追加
        System.out.println("体重が送信されました: " + weight + ", 日付: " + recordedDateStr);
        
        try {
            // 仮のユーザーID
            Integer userId = 1;
            
            // 文字列からLocalDate型への変換
            LocalDate recordedDate = LocalDate.parse(recordedDateStr);
            
            // 選択された日付を使用して体重記録を保存
            weightService.saveWeightRecord(userId, weight, recordedDate);
            return "redirect:/";
        } catch (Exception e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/?error=true";
        }
    }
    
    // 体重記録を削除（CRUD機能の一部）
    @GetMapping("/delete/{id}")
    public String deleteWeight(@PathVariable("id") Long id) {
        try {
            weightService.deleteWeightRecord(id);
        } catch (Exception e) {
            System.err.println("削除エラー: " + e.getMessage());
        }
        return "redirect:/";
    }
    // （平田）CSVエクスポート機能
    // URLでアクセスされるようにする
    @GetMapping("/export")
    public void exportCSV(HttpServletResponse response) throws IOException {
        // ブラウザにCSVだと教える
        response.setContentType("text/csv");
        // CSVファイル名を指定
        response.setHeader("Content-Disposition", "attachment; filename=\"weight_records.csv\"");

        // データベースから体重記録を取得する
        List<WeightRecord> records = weightService.getAllWeightRecords();

        // 書き出し用のペンを準備
        PrintWriter writer = response.getWriter();
        // CSVのヘッダーを書き込む("ID,体重,記録日")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");//追記
        writer.println("ID,UserID,Weight,Timestamp");

        // データを1行ずつ書き出す
        for (WeightRecord record : records) {
            writer.println(String.format("%d,%d,%.2f,%s", 
                record.getId(), 
                record.getUserId(), 
                record.getWeight(), 
                record.getTimestamp()));

        }
        writer.flush(); // 溜まってるデータを出し切る
        writer.close(); // 道具をちゃんと閉じる

    }
    
}