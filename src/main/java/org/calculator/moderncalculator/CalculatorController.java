package org.calculator.moderncalculator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.text.DecimalFormat;

/**
 *CalculatorController类负责处理计算器的所有逻辑。
 *它响应用户通过GUI进行的操作，并更新显示。
 */
public class CalculatorController
{

    //FXML注解用于将代码中的变量与FXML文件中定义的UI元素关联起来
    @FXML
    private TextField displayField; //主显示屏，显示当前输入或结果
    @FXML
    private TextField historyDisplayField; //历史记录显示屏，显示计算过程

    //计算器状态变量
    private String currentInputValue = "0";   //当前用户输入的数字字符串，默认为 "0"
    private String historyLog = "";           //存储历史计算表达式的字符串
    private double firstOperand = 0;          //第一个操作数
    private String pendingOperator = "";      //等待执行的操作符(+, -, ×, ÷, %)
    private boolean isAwaitingSecondOperand = false; //标记是否在输入第一个操作数和操作符后，等待输入第二个操作数
    private boolean resultJustDisplayed = true;   //标记当前显示的是否是上一次计算的结果

    /**
     *初始化方法，在FXML加载完成后自动调用。
     *用于设置计算器的初始状态。
     */
    @FXML
    public void initialize()
    {
        updateDisplays(); //初始化时更新显示内容
    }

    /**
     *处理数字按钮点击事件。
     *@param event 点击事件对象，可以从中获取被点击的按钮
     */
    @FXML
    private void handleDigitAction(ActionEvent event)
    {
        //如果显示屏正显示错误信息，则不处理数字输入
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        String digit = ((Button) event.getSource()).getText(); //获取被点击按钮上的数字

        //如果刚显示完结果，并且用户开始输入数字，则重置当前输入值为新数字
        if (resultJustDisplayed)
        {
            currentInputValue = digit;
            resultJustDisplayed = false; //不再是“刚显示完结果”的状态
            //如果不是在等待第二个操作数（即开始新的独立计算），则清空历史记录
            if (!isAwaitingSecondOperand)
            {
                historyLog = "";
            }
        }
        else //如果不是刚显示完结果，则追加数字
        {
            //处理前导零：如果当前输入是"0"且新数字不是"."，则替换"0"
            if (currentInputValue.equals("0") && !digit.equals("."))
            {
                currentInputValue = digit;
            }
            //处理"-0"的情况
            else if (currentInputValue.equals("-0") && !digit.equals("."))
            {
                currentInputValue = "-" + digit;
            }
            else {
                //限制输入长度，防止溢出或显示问题
                String temp = currentInputValue.startsWith("-") ? currentInputValue.substring(1) : currentInputValue;
                if (temp.replace(".", "").length() < 15)
                {
                    currentInputValue += digit;
                }
            }
        }
        updateDisplays(); //更新显示
    }

    /**
     *处理操作符按钮 (+, -, ×, ÷, %) 点击事件。
     *@param event 点击事件对象
     */
    @FXML
    private void handleOperatorAction(ActionEvent event)
    {
        String newOperator = ((Button) event.getSource()).getText(); //获取操作符
        //如果显示屏正显示错误信息，则不处理
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        //如果不是刚显示完结果，并且正在等待第二个操作数 (例如: 5 + 3 - , 按下-时)
        //且当前输入是有效数字，则先计算之前的结果
        if (!resultJustDisplayed && isAwaitingSecondOperand)
        {
            if (canParseAsNumber(currentInputValue))
            {
                calculate(); //计算 5 + 3
            }
            //如果当前输入不是有效数字（例如只输入了"-")，则忽略
            else
            {
            }
        }
        try
        {
            //防止单独输入"-"后直接按操作符
            if (currentInputValue.equals("-"))
            {
                displayError("无效输入");
                return;
            }
            firstOperand = Double.parseDouble(currentInputValue); //将当前输入转为第一个操作数
        }
        catch (NumberFormatException e)
        {
            displayError("错误: 无效数字"); //如果转换失败，显示错误
            return;
        }
        pendingOperator = newOperator; //设置等待执行的操作符
        historyLog = formatResult(firstOperand) + " " + pendingOperator; //更新历史记录，例如"5 +"
        isAwaitingSecondOperand = true; //设置为等待第二个操作数状态
        resultJustDisplayed = true;   //准备接收第二个操作数
        updateDisplays(); //更新显示
    }

    /**
     *处理等号 (=) 按钮点击事件。
     *@param event 点击事件对象
     */
    @FXML
    private void handleEqualsAction(ActionEvent event)
    {
        //如果显示屏正显示错误信息，则不处理
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        //如果没有等待执行的操作符(例如用户只输入了 "5" 然后按 "=")
        if (pendingOperator.isEmpty())
        {
            historyLog = currentInputValue + " ="; //历史记录显示 "5 ="
            resultJustDisplayed = true; //标记结果已显示
            updateDisplays();
            return;
        }
        //防止当前输入为"-"时按等号
        if (currentInputValue.equals("-"))
        {
            displayError("无效输入");
            return;
        }
        calculate(); //执行计算
    }

    /**
     *执行实际的计算过程。
     *根据pendingOperator对firstOperand和currentInputValue(作为第二个操作数)进行运算。
     */
    private void calculate()
    {
        //如果没有挂起的操作符，则不执行任何操作
        if (pendingOperator.isEmpty())
        {
            return;
        }

        double secondOperand;
        String secondOperandStrForHistory = currentInputValue; //用于历史记录的第二个操作数

        try
        {
            //特殊情况：如果刚显示完结果并且在等待第二个操作数（例如：5+然后按=，此时会用5作为第二个操作数，即 5+5）
            if (resultJustDisplayed && isAwaitingSecondOperand)
            {
                //如果是连续按等号，firstOperand已经是上次的结果，currentInputValue是上次的第二个操作数
                //所以secondOperand = firstOperand
                secondOperand = firstOperand;
                secondOperandStrForHistory = formatResult(firstOperand); //历史记录也用格式化后的第一个操作数
            }
            else
            {
                //正常情况：解析当前输入值为第二个操作数
                if (currentInputValue.equals("-")) //防止单独"-"作为操作数
                {
                    displayError("无效输入");
                    return;
                }
                secondOperand = Double.parseDouble(currentInputValue);
            }
        }
        catch (NumberFormatException e)
        {
            displayError("错误: 无效数字"); //解析失败则显示错误
            return;
        }

        //更新历史记录，显示完整的计算表达式，例如"5 + 3 ="
        historyLog = formatResult(firstOperand) + " " + pendingOperator + " " + secondOperandStrForHistory + " =";
        double resultValue = 0;
        boolean error = false; //错误标记

        //根据等待的操作符执行相应的计算
        switch (pendingOperator)
        {
            case "+":
                resultValue = firstOperand + secondOperand;
                break;
            case "-":
                resultValue = firstOperand - secondOperand;
                break;
            case "×":
                resultValue = firstOperand * secondOperand;
                break;
            case "÷":
                if (secondOperand == 0) //除数不能为零
                {
                    displayError("除数不能为零");
                    error = true;
                }
                else
                {
                    resultValue = firstOperand / secondOperand;
                }
                break;
            case "%": //求余操作
                if (secondOperand == 0)
                {
                    displayError("模数不能为零");
                    error = true;
                }
                else
                {
                    resultValue = firstOperand % secondOperand;
                }
                break;
            default:
                return;
        }

        if (!error) //如果没有发生错误
        {
            currentInputValue = formatResult(resultValue); //将计算结果格式化后设为当前输入值
            firstOperand = resultValue; //将结果保存为下一次计算的第一个操作数
        }
        resultJustDisplayed = true; //标记结果已显示
        isAwaitingSecondOperand = false;
        updateDisplays(); //更新显示
    }
    /**
     *处理 "C" (Clear All) 按钮点击事件。
     *重置计算器到初始状态。
     *@param event 点击事件对象
     */
    @FXML
    private void handleClearAction(ActionEvent event)
    {
        currentInputValue = "0";      //当前输入重置为 "0"
        historyLog = "";              //清空历史记录
        firstOperand = 0;             //第一个操作数重置为 0
        pendingOperator = "";         //清空等待的操作符
        isAwaitingSecondOperand = false; //不再等待第二个操作数
        resultJustDisplayed = true;   //恢复到初始“结果已显示”状态（虽然是0）
        updateDisplays();             //更新显示
    }
    /**
     *处理 "CE" (Clear Entry) 按钮点击事件。
     *清除当前输入项。
     *@param event 点击事件对象
     */
    @FXML
    private void handleClearEntryAction(ActionEvent event)
    {
        //如果显示屏正显示错误信息，CE的行为等同于 C
        if (isDisplayShowingError(displayField.getText()))
        {
            handleClearAction(event); //调用C的处理逻辑
            return;
        }

        currentInputValue = "0"; //当前输入重置为 "0"
        //如果不是在等待第二个操作数
        //那么清除历史记录和挂起的操作符，因为当前条目被清除了，之前的半成品表达式也应无效。
        if (!isAwaitingSecondOperand)
        {
            historyLog = "";
            pendingOperator = "";
        }
        //如果正在等待第二个操作数(例如5 +然后按CE)，则historyLog(5 +)应该保留。
        resultJustDisplayed = true; //将状态设为准备接收新输入
        updateDisplays(); //更新显示
    }
    /**
     *处理退格 (Backspace) 按钮点击事件。
     *@param event 点击事件对象
     */
    @FXML
    private void handleBackspaceAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        //如果刚显示完结果，且不是在等待第二个操作数（例如"5 + 3 = 8"，此时按退格）
        //并且有挂起的操作符
        //且历史记录不是以操作符结尾（历史记录是完整的"a op b ="或"a ="）
        if (resultJustDisplayed && !isAwaitingSecondOperand && !pendingOperator.isEmpty() && !historyLog.endsWith(pendingOperator))
        {
            String[] parts = historyLog.split(" "); //按空格分割历史记录
            //情况1:历史记录是"firstOp op secondOp =" (例如"5 + 3 =")
            if (parts.length >= 4 && parts[parts.length - 1].equals("="))
            {
                try
                {
                    //尝试恢复到"="按下之前的状态
                    firstOperand = Double.parseDouble(parts[0]);
                    pendingOperator = parts[1];
                    currentInputValue = parts[2]; //第二个操作数恢复到当前输入
                    historyLog = formatResult(firstOperand) + " " + pendingOperator + " " + currentInputValue; //恢复历史"5 + 3"
                    isAwaitingSecondOperand = true; //设置为等待第二个操作数
                    resultJustDisplayed = false; //不再是“刚显示结果”状态，允许修改currentInputValue
                    updateDisplays();
                    return;
                }
                catch (Exception e) //解析失败则回退到更简单的状态
                {
                    currentInputValue = "0";
                    resultJustDisplayed = true;
                    if (!isAwaitingSecondOperand) historyLog = "";
                    updateDisplays();
                    return;
                }
            }
            // 情况2:历史记录是"operand ="(例如"5 =")
            else if (parts.length == 2 && parts[1].equals("="))
            {
                currentInputValue = parts[0]; //操作数恢复到当前输入
                historyLog = ""; //清空历史
                pendingOperator = ""; //清空操作符
                resultJustDisplayed = false; //允许修改currentInputValue
                updateDisplays();
                return;
            }
        }
        //如果正在等待第二个操作数，并且刚显示完结果（例如按了"5 +"后，显示屏是"5"历史是"5 +"此时按退格）
        //目的是撤销操作符
        if (isAwaitingSecondOperand && resultJustDisplayed && historyLog.endsWith(pendingOperator))
        {
            currentInputValue = formatResult(firstOperand); //当前输入恢复为第一个操作数
            historyLog = ""; //清空历史
            pendingOperator = ""; //清空操作符
            isAwaitingSecondOperand = false; //不再等待第二个操作数
            resultJustDisplayed = false; //允许修改currentInputValue
            updateDisplays();
            return;
        }
        //如果不是刚显示完结果（即用户正在输入数字）
        if (!resultJustDisplayed)
        {
            if (!currentInputValue.isEmpty() && !currentInputValue.equals("0"))
            {
                //特殊处理：如果正在等待第二个操作数，历史记录以操作符结尾，且当前输入只有一个非零数字
                if (isAwaitingSecondOperand && historyLog.endsWith(pendingOperator)
                        && currentInputValue.length() == 1 && !currentInputValue.equals("0"))
                {
                    currentInputValue = "0"; //将当前输入设为"0"而不是空字符串
                }
                else
                {
                    currentInputValue = currentInputValue.substring(0, currentInputValue.length() - 1); //删除最后一个字符
                    //如果删除后为空或只剩负号，则设为"0"
                    if (currentInputValue.isEmpty() || currentInputValue.equals("-"))
                    {
                        currentInputValue = "0";
                    }
                }
            }
            else //如果当前输入是空或"0"，则设为"0"
            {
                currentInputValue = "0";
            }
            updateDisplays();
            return;
        }
        //其他一般情况或退格到初始状态
        currentInputValue = "0";
        if (!isAwaitingSecondOperand) //如果不是在输入第二个操作数，则清空历史
        {
            historyLog = "";
        }
        updateDisplays();
    }
    /**
     *处理小数点 (.) 按钮点击事件。
     *@param event 点击事件对象
     */
    @FXML
    private void handleDecimalAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        //如果刚显示完结果，用户按小数点，则开始新的输入 "0."
        if (resultJustDisplayed)
        {
            currentInputValue = "0.";
            resultJustDisplayed = false; //不再是“刚显示完结果”的状态
            //如果不是在等待第二个操作数（即开始新的独立计算），则清空历史记录
            if (!isAwaitingSecondOperand)
            {
                historyLog = "";
            }
        }
        //如果当前输入中不包含小数点
        else if (!currentInputValue.contains("."))
        {
            if (currentInputValue.isEmpty()) //如果当前输入为空（理论上不应发生，因为会是"0"）
            {
                currentInputValue = "0.";
            }
            else if (currentInputValue.equals("-")) //如果当前是"-"
            {
                currentInputValue += "0."; //变为 "-0."
            }
            else //正常追加小数点
            {
                currentInputValue += ".";
            }
        }
        updateDisplays(); //更新显示
    }
    /**
     *处理正负号 (+/-) 按钮点击事件。
     *@param event 点击事件对象
     */
    @FXML
    private void handleSignAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        //如果以"-"开头，则去掉"-"
        if (currentInputValue.startsWith("-"))
        {
            currentInputValue = currentInputValue.substring(1);
        }
        //否则，在前面加上 "-"
        else
        {
            currentInputValue = "-" + currentInputValue;
        }
        //如果是结果显示后按正负号，且不是等待第二个操作数，意味着改变的是刚算出来的结果
        //这个结果会作为下一次运算的firstOperand
        if (resultJustDisplayed && !isAwaitingSecondOperand)
        {
            try
            {
                firstOperand = Double.parseDouble(currentInputValue);
            }
            catch (NumberFormatException ignored)
            {
                displayError("错误: 无效数字");
                return;
            }
        }
        updateDisplays(); //更新显示
    }
    /**
     *处理百分号 (%) 按钮点击事件。
     *行为依赖于上下文
     *1.如果是"A + B %"，则B%计算为A * (B/100)并作为第二个操作数与A相加。
     *2.如果是"A * B %"或"A / B %"，则B%计算为B/100并作为第二个操作数与A相乘/除。
     *3.如果只是"A %"，则计算A/100。
     *@param event 点击事件对象
     */
    @FXML
    private void handlePercentageAction(ActionEvent event)
    {
        //错误或无效输入检查
        if (isDisplayShowingError(displayField.getText()) || currentInputValue.isEmpty() || currentInputValue.equals("-"))
        {
            return;
        }
        double valueInCurrentInput;
        try
        {
            valueInCurrentInput = Double.parseDouble(currentInputValue); //解析当前输入
        }
        catch (NumberFormatException e)
        {
            displayError("错误");
            return;
        }
        //如果有挂起的操作符并且正在等待第二个操作数(例如:X + Y%)
        if (!pendingOperator.isEmpty() && isAwaitingSecondOperand)
        {
            double percentageResultValue;
            String originalSecondOperandForHistory = currentInputValue; //保存原始Y，用于历史记录
            //根据操作符类型决定百分比的计算方式
            if (pendingOperator.equals("+") || pendingOperator.equals("-"))
            {
                //对于加减法，百分比是相对于第一个操作数的(例如 100 + 10% = 100 + 100*0.1 = 110)
                percentageResultValue = firstOperand * (valueInCurrentInput / 100.0);
            }
            else if (pendingOperator.equals("×") || pendingOperator.equals("÷"))
            {
                //对于乘除法，百分比是操作数本身除以100 (例如 100 * 10% = 100 * 0.1 = 10)
                percentageResultValue = valueInCurrentInput / 100.0;
            }
            else //其他操作符（例如 %），行为可能未定义或同乘除
            {
                percentageResultValue = valueInCurrentInput / 100.0; //默认为自身百分比
            }
            //更新历史记录 (例如:"100 + 10%")
            historyLog = formatResult(firstOperand) + " " + pendingOperator + " " + originalSecondOperandForHistory + "%";
            currentInputValue = formatResult(percentageResultValue); //将百分比计算结果设为当前输入
        }
        else //如果没有挂起的操作符 (例如:Y%)
        {
            historyLog = currentInputValue + "%"; //历史记录 "Y%"
            currentInputValue = formatResult(valueInCurrentInput / 100.0); //计算 Y/100
            historyLog += " ="; //历史记录"Y% ="
            resultJustDisplayed = true; //标记结果已显示
        }
        updateDisplays(); //更新显示
    }
    /**
     *处理倒数 (1/x) 按钮点击事件。
     *@param event 点击事件对象
     */
    @FXML
    private void handleReciprocalAction(ActionEvent event)
    {
        //错误或无效输入检查
        if (isDisplayShowingError(displayField.getText()) || currentInputValue.isEmpty() || currentInputValue.equals("-"))
        {
            return;
        }
        try
        {
            double value = Double.parseDouble(currentInputValue);
            if (value == 0) //除数不能为零
            {
                displayError("除数不能为零");
            }
            else
            {
                historyLog = "1/(" + formatResult(value) + ")"; //更新历史记录"1/(value)"
                value = 1.0 / value; //计算倒数
                currentInputValue = formatResult(value); //将结果设为当前输入
                historyLog += " ="; //更新历史记录"1/(value) ="
                firstOperand = value; //将结果保存为第一个操作数
                resultJustDisplayed = true; //标记结果已显示
                pendingOperator = ""; //清空操作符
                isAwaitingSecondOperand = false;
                updateDisplays(); //更新显示
            }
        }
        catch (NumberFormatException e)
        {
            displayError("错误");
        }
    }
    /**
     *处理一元运算按钮点击事件 (例如 x², √x)。
     *@param event 点击事件对象
     */
    @FXML
    private void handleUnaryOperationAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()) || currentInputValue.isEmpty() || currentInputValue.equals("-"))
        {
            return;
        }
        String operationSymbol = ((Button) event.getSource()).getText(); //获取操作符号(x², ²√x)
        String historyOpName; //用于历史记录的操作名
        //根据按钮文本确定历史记录中的操作名
        if (operationSymbol.equals("x²"))
        {
            historyOpName = "sqr"; //平方
        }
        else if (operationSymbol.equals("²√x"))
        {
            historyOpName = "sqrt"; //平方根
        }
        else
        {
            return; //未知一元操作
        }
        double value;
        try
        {
            value = Double.parseDouble(currentInputValue);
        }
        catch (NumberFormatException e)
        {
            displayError("错误");
            return;
        }
        historyLog = historyOpName + "(" + formatResult(value) + ")"; //更新历史记录，例如"sqr(5)"
        double resultValue = 0;
        boolean error = false; //错误标记
        //根据操作符号执行计算
        switch (operationSymbol)
        {
            case "x²":
                resultValue = value * value; //计算平方
                break;
            case "²√x":
                if (value < 0) //负数不能开平方根
                {
                    displayError("无效输入");
                    error = true;
                }
                else
                {
                    resultValue = Math.sqrt(value); //计算平方根
                }
                break;
        }
        if (!error)
        {
            currentInputValue = formatResult(resultValue); //将结果设为当前输入
            historyLog += " ="; //更新历史记录，例如"sqr(5) ="
            firstOperand = resultValue; //将结果保存为第一个操作数
            resultJustDisplayed = true; //标记结果已显示
            pendingOperator = ""; //清空操作符
            isAwaitingSecondOperand = false;
            updateDisplays();
        }
    }
    /**
     *更新主显示屏和历史记录显示屏的内容。
     *这个方法会在每次计算器状态改变后被调用，以刷新UI。
     */
    private void updateDisplays()
    {
        String mainText = currentInputValue;
        displayField.setText(mainText); //设置主显示屏文本
        String historyText = historyLog;
        historyDisplayField.setText(historyText); //设置历史记录显示屏文本
    }
    /**
     *格式化计算结果以便显示。
     *例如，移除不必要的小数点后的零 (5.0 -> 5)。
     *处理 NaN 和 Infinity。
     *@param result 要格式化的数字
     *@return 格式化后的字符串
     */
    private String formatResult(double result)
    {
        //处理特殊数字情况
        if (Double.isNaN(result))
        {
            return "结果未定义";
        }
        if (Double.isInfinite(result))
        {
            return "溢出"; //Infinity
        }

        double epsilon = 1E-10; //一个很小的数，用于比较浮点数是否接近整数
        //如果数字非常接近一个整数(例如 4.9999999999 或 5.0000000001)
        if (Math.abs(result - Math.round(result)) < epsilon && result != 0)
        {
            return String.format("%d", Math.round(result)); //返回整数形式
        }
        //如果数字非常接近0但不完全是0(例如 0.00000000001)
        else if (Math.abs(result) < epsilon && result !=0)
        {
            return "0"; //统一显示为"0"
        }
        else //其他情况，保留小数
        {
            //使用DecimalFormat格式化，最多保留10位小数，并去除末尾的0
            DecimalFormat df = new DecimalFormat("#.##########");
            String formatted = df.format(result);
            //DecimalFormat对于(-1, 0)之间的小数可能格式化为"-,xxxx"或",xxxx"
            //需要修正为"-0.xxxx"或"0.xxxx"
            if (formatted.startsWith("-,")) //例如-0.5会被格式化为-,5
            {
                formatted = "-0." + formatted.substring(2); //改为-0.5
            }
            else if (formatted.startsWith(",")) //例如0.5会被格式化为,5
            {
                formatted = "0." + formatted.substring(1); //改为0.5
            }
            //处理-0的情况
            if (formatted.equals("-0"))
            {
                return "0";
            }
            return formatted;
        }
    }
    /**
     *检查字符串是否可以被解析为有效的数字。
     *@param s 要检查的字符串
     *@return 如果可以解析为数字则返回 true，否则 false
     */
    private boolean canParseAsNumber(String s)
    {
        if (s == null || s.isEmpty() || s.equals("-") || s.equals(".") || s.equals("-.")) {
            return false;
        }
        try
        {
            Double.parseDouble(s);
            return true;
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }
    /**
     *在主显示屏上显示错误信息，并重置计算器状态（部分）。
     *@param message 要显示的错误信息
     */
    private void displayError(String message)
    {
        displayField.setText(message);   //在主显示屏显示错误
        historyLog = "";                 //清空历史记录
        currentInputValue = "0";         //当前输入重置为 "0"
        firstOperand = 0;                //重置第一个操作数
        pendingOperator = "";            //清空等待的操作符
        isAwaitingSecondOperand = false;  //不再等待第二个操作数
        resultJustDisplayed = true;     //标记为“结果已显示”状态，以便下次输入数字时能覆盖错误信息
    }
    /**
     *检查显示屏当前是否正在显示错误信息。
     *@param displayText 显示屏的文本内容
     *@return 如果是错误信息则返回 true，否则 false
     */
    private boolean isDisplayShowingError(String displayText)
    {
        if (displayText == null)
        {
            return false;
        }
        String text = displayText.toLowerCase(); //转为小写以便不区分大小写比较
        //检查是否包含常见的错误关键词
        return text.contains("错误") || text.contains("error") || text.contains("nan") ||
                text.contains("溢出") || text.contains("未定义") || text.contains("除数不能为零") ||
                text.contains("无效输入") || text.contains("模数不能为零");
    }
}