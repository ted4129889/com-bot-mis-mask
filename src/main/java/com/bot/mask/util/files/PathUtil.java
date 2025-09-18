/* (C) 2023 */
package com.bot.mask.util.files;

import com.bot.mask.log.LogProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Scope("prototype")
public class PathUtil {
    /**
     * 將輸入檔案路徑進行轉換：
     * 1. 替換路徑中的 "input" → "output_mask_datafile"
     * 2. 如果檔案名稱以 "Misbh_Fas" 或 "Fas" 開頭，就在檔名主體末尾加上 "Conv"
     *
     * @param reNamePath 原始檔案路徑（字串），如 D:/.../input/Misbh_FasABC.1140224.txt
     * @return 轉換後的路徑字串
     */
    public  String convertToMaskedOutputPath(String reNamePath) {
        // 步驟 1：將 "input" 替換為 "output_mask_datafile"
//        String reNamePath = requestedFilePath.replace("input", "output_mask_datafile");

        // 步驟 2：找到檔名開始的位置
        int lastSeparatorIndex = reNamePath.lastIndexOf('/');
        if (lastSeparatorIndex == -1) {
            lastSeparatorIndex = reNamePath.lastIndexOf('\\'); // 處理 Windows 路徑
        }

        // 分離路徑與檔名
        String dirPath = lastSeparatorIndex >= 0 ? reNamePath.substring(0, lastSeparatorIndex + 1) : "";
        String fileName = lastSeparatorIndex >= 0 ? reNamePath.substring(lastSeparatorIndex + 1) : reNamePath;

        // 分離檔名主體與副檔名
        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExt = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        String extension = dotIndex > 0 ? fileName.substring(dotIndex) : "";

        // 如果檔名主體以 "Misbh_Fas" 或 "Fas" 開頭，就加上 "Conv"
        String comfirmTxt =nameWithoutExt;
        if (nameWithoutExt.toLowerCase().startsWith("misbh_fas") || nameWithoutExt.toLowerCase().startsWith("fas")) {
            comfirmTxt += "Conv";
        }
        // 組合最終完整路徑
        return dirPath + comfirmTxt + extension;
    }

}
