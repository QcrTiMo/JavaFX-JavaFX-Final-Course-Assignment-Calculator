package org.calculator.moderncalculator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.text.DecimalFormat;

public class CalculatorController
{

    @FXML
    private TextField displayField;
    @FXML
    private TextField historyDisplayField;
    private String currentInputValue = "0";
    private String historyLog = "";
    private double firstOperand = 0;
    private String pendingOperator = "";
    private boolean isAwaitingSecondOperand = false;
    private boolean resultJustDisplayed = true;

    @FXML
    public void initialize()
    {
        updateDisplays();
    }

    @FXML
    private void handleDigitAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        String digit = ((Button) event.getSource()).getText();
        if (resultJustDisplayed)
        {
            currentInputValue = digit;
            resultJustDisplayed = false;
            if (!isAwaitingSecondOperand)
            {
                historyLog = "";
            }
        }
        else
        {
            if (currentInputValue.equals("0") && !digit.equals("."))
            {
                currentInputValue = digit;
            }
            else if (currentInputValue.equals("-0") && !digit.equals("."))
            {
                currentInputValue = "-" + digit;
            }
            else {
                String temp = currentInputValue.startsWith("-") ? currentInputValue.substring(1) : currentInputValue;
                if (temp.replace(".", "").length() < 15)
                {
                    currentInputValue += digit;
                }
            }
        }
        updateDisplays();
    }

    @FXML
    private void handleOperatorAction(ActionEvent event)
    {
        String newOperator = ((Button) event.getSource()).getText();
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        if (!resultJustDisplayed && isAwaitingSecondOperand)
        {
            if (canParseAsNumber(currentInputValue))
            {
                calculate();
            }
            else
            {
            }
        }
        try
        {
            if (currentInputValue.equals("-"))
            {
                displayError("无效输入");
                return;
            }
            firstOperand = Double.parseDouble(currentInputValue);
        }
        catch (NumberFormatException e)
        {
            displayError("错误: 无效数字");
            return;
        }
        pendingOperator = newOperator;
        historyLog = formatResult(firstOperand) + " " + pendingOperator;
        isAwaitingSecondOperand = true;
        resultJustDisplayed = true;
        updateDisplays();
    }

    @FXML
    private void handleEqualsAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        if (pendingOperator.isEmpty())
        {
            historyLog = currentInputValue + " =";
            resultJustDisplayed = true;
            updateDisplays();
            return;
        }
        if (currentInputValue.equals("-"))
        {
            displayError("无效输入");
            return;
        }
        calculate();
    }

    private void calculate()
    {
        if (pendingOperator.isEmpty())
        {
            return;
        }
        double secondOperand;
        String secondOperandStrForHistory = currentInputValue;
        try
        {
            if (resultJustDisplayed && isAwaitingSecondOperand)
            {
                secondOperand = firstOperand;
                secondOperandStrForHistory = formatResult(firstOperand);
            }
            else
            {
                if (currentInputValue.equals("-"))
                {
                    displayError("无效输入");
                    return;
                }
                secondOperand = Double.parseDouble(currentInputValue);
            }
        }
        catch (NumberFormatException e)
        {
            displayError("错误: 无效数字");
            return;
        }
        historyLog = formatResult(firstOperand) + " " + pendingOperator + " " + secondOperandStrForHistory + " =";
        double resultValue = 0;
        boolean error = false;
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
                if (secondOperand == 0)
                {
                    displayError("除数不能为零"); error = true;
                }
                else
                {
                    resultValue = firstOperand / secondOperand;
                }
                break;
            case "%":
                if (secondOperand == 0)
                {
                    displayError("模数不能为零"); error = true;
                }
                else
                {
                    resultValue = firstOperand % secondOperand;
                }
                break;
            default: return;
        }
        if (!error)
        {
            currentInputValue = formatResult(resultValue);
            firstOperand = resultValue;
        }
        resultJustDisplayed = true;
        isAwaitingSecondOperand = false;
        updateDisplays();
    }

    @FXML
    private void handleClearAction(ActionEvent event)
    {
        currentInputValue = "0";
        historyLog = "";
        firstOperand = 0;
        pendingOperator = "";
        isAwaitingSecondOperand = false;
        resultJustDisplayed = true;
        updateDisplays();
    }

    @FXML
    private void handleClearEntryAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            handleClearAction(event);
            return;
        }
        currentInputValue = "0";
        if (!isAwaitingSecondOperand)
        {
            historyLog = "";
            pendingOperator = "";
        }
        resultJustDisplayed = true;
        updateDisplays();
    }

    @FXML
    private void handleBackspaceAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }
        if (resultJustDisplayed && !isAwaitingSecondOperand && !pendingOperator.isEmpty() && !historyLog.endsWith(pendingOperator))
        {
            String[] parts = historyLog.split(" ");
            if (parts.length >= 4 && parts[parts.length - 1].equals("="))
            {
                try
                {
                    firstOperand = Double.parseDouble(parts[0]);
                    pendingOperator = parts[1];
                    currentInputValue = parts[2];
                    String lastSecondOperandStrBeforeEquals = currentInputValue;
                    historyLog = formatResult(firstOperand) + " " + pendingOperator + " " + currentInputValue;
                    isAwaitingSecondOperand = true;
                    resultJustDisplayed = false;
                    updateDisplays();
                    return;
                }
                catch (Exception e)
                {
                    currentInputValue = "0";
                    resultJustDisplayed = true;
                    if (!isAwaitingSecondOperand) historyLog = "";
                    updateDisplays();
                    return;
                }
            }
            else if (parts.length == 2 && parts[1].equals("="))
            {
                currentInputValue = parts[0];
                historyLog = "";
                pendingOperator = "";
                resultJustDisplayed = false;
                updateDisplays();
                return;
            }
        }
        if (isAwaitingSecondOperand && resultJustDisplayed && historyLog.endsWith(pendingOperator))
        {
            currentInputValue = formatResult(firstOperand);
            historyLog = "";
            pendingOperator = "";
            isAwaitingSecondOperand = false;
            resultJustDisplayed = false;
            updateDisplays();
            return;
        }

        if (!resultJustDisplayed)
        {
            if (!currentInputValue.isEmpty() && !currentInputValue.equals("0"))
            {
                if (isAwaitingSecondOperand && historyLog.endsWith(pendingOperator)
                        && currentInputValue.length() == 1 && !currentInputValue.equals("0"))
                {
                    currentInputValue = "0";
                }
                else
                {
                    currentInputValue = currentInputValue.substring(0, currentInputValue.length() - 1);
                    if (currentInputValue.isEmpty() || currentInputValue.equals("-"))
                    {
                        currentInputValue = "0";
                    }
                }
            }
            else
            {
                currentInputValue = "0";
            }
            updateDisplays();
            return;
        }
        currentInputValue = "0";
        if (!isAwaitingSecondOperand)
        {
            historyLog = "";
        }
        updateDisplays();
    }

    @FXML
    private void handleDecimalAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }

        if (resultJustDisplayed)
        {
            currentInputValue = "0.";
            resultJustDisplayed = false;
            if (isAwaitingSecondOperand)
            {
            }
            else
            {
                historyLog = "";
            }
        }
        else if (!currentInputValue.contains("."))
        {
            if (currentInputValue.isEmpty())
            {
                currentInputValue = "0.";
            }
            else if (currentInputValue.equals("-"))
            {
                currentInputValue += "0.";
            }
            else
            {
                currentInputValue += ".";
            }
        }
        updateDisplays();
    }

    @FXML
    private void handleSignAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()))
        {
            return;
        }

        if (currentInputValue.equals("0") || currentInputValue.equals("0.0"))
        {
        }
        else if (currentInputValue.startsWith("-"))
        {
            currentInputValue = currentInputValue.substring(1);
        }
        else
        {
            currentInputValue = "-" + currentInputValue;
        }

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
        updateDisplays();
    }

    @FXML
    private void handlePercentageAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()) || currentInputValue.isEmpty() || currentInputValue.equals("-"))
        {
            return;
        }
        double valueInCurrentInput;
        try
        {
            valueInCurrentInput = Double.parseDouble(currentInputValue);
        }
        catch (NumberFormatException e)
        {
            displayError("错误");
            return;
        }

        if (!pendingOperator.isEmpty() && isAwaitingSecondOperand)
        {
            double percentageResultValue;
            String originalSecondOperandForHistory = currentInputValue;
            if (pendingOperator.equals("+") || pendingOperator.equals("-"))
            {
                percentageResultValue = firstOperand * (valueInCurrentInput / 100.0);
            }
            else if (pendingOperator.equals("×") || pendingOperator.equals("÷"))
            {
                percentageResultValue = valueInCurrentInput / 100.0;
            }
            else
            {
                percentageResultValue = valueInCurrentInput / 100.0;
            }
            historyLog = formatResult(firstOperand) + " " + pendingOperator + " " + originalSecondOperandForHistory + "%";
            currentInputValue = formatResult(percentageResultValue);
        }
        else
        {
            historyLog = currentInputValue + "%";
            currentInputValue = formatResult(valueInCurrentInput / 100.0);
            historyLog += " =";
            resultJustDisplayed = true;
        }
        updateDisplays();
    }

    @FXML
    private void handleReciprocalAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()) || currentInputValue.isEmpty() || currentInputValue.equals("-"))
        {
            return;
        }
        try
        {
            double value = Double.parseDouble(currentInputValue);
            if (value == 0)
            {
                displayError("除数不能为零");
            }
            else
            {
                historyLog = "1/(" + formatResult(value) + ")";
                value = 1.0 / value;
                currentInputValue = formatResult(value);
                historyLog += " =";
                firstOperand = value;
                resultJustDisplayed = true;
                pendingOperator = "";
                isAwaitingSecondOperand = false;
                updateDisplays();
            }
        }
        catch (NumberFormatException e)
        {
            displayError("错误");
        }
    }

    @FXML
    private void handleUnaryOperationAction(ActionEvent event)
    {
        if (isDisplayShowingError(displayField.getText()) || currentInputValue.isEmpty() || currentInputValue.equals("-"))
        {
            return;
        }
        String operationSymbol = ((Button) event.getSource()).getText();
        String historyOpName;
        if (operationSymbol.equals("x²"))
        {
            historyOpName = "sqr";
        }
        else if (operationSymbol.equals("²√x"))
        {
            historyOpName = "sqrt";
        }
        else
        {
            return;
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

        historyLog = historyOpName + "(" + formatResult(value) + ")";

        double resultValue = 0;
        boolean error = false;
        switch (operationSymbol)
        {
            case "x²":
                resultValue = value * value;
                break;
            case "²√x":
                if (value < 0)
                {
                    displayError("无效输入");
                    error = true;
                }
                else
                {
                    resultValue = Math.sqrt(value);
                }
                break;
        }
        if (!error)
        {
            currentInputValue = formatResult(resultValue);
            historyLog += " =";
            firstOperand = resultValue;
            resultJustDisplayed = true;
            pendingOperator = "";
            isAwaitingSecondOperand = false;
            updateDisplays();
        }
    }

    private void updateDisplays()
    {
        String mainText = currentInputValue;
        displayField.setText(mainText);
        String historyText = historyLog;
        historyDisplayField.setText(historyText);
    }

    private String formatResult(double result)
    {
        if (Double.isNaN(result))
        {
            return "结果未定义";
        }
        if (Double.isInfinite(result))
        {
            return "溢出";
        }
        double epsilon = 1E-10;
        if (Math.abs(result - Math.round(result)) < epsilon && result != 0)
        {
            return String.format("%d", Math.round(result));
        }
        else if (Math.abs(result) < epsilon && result !=0)
        {
            return "0";
        }
        else
        {
            DecimalFormat df = new DecimalFormat("#.##########");
            String formatted = df.format(result);
            if (formatted.startsWith("-,"))
            {
                formatted = "-0." + formatted.substring(2);
            }
            else if (formatted.startsWith(","))
            {
                formatted = "0." + formatted.substring(1);
            }
            if (formatted.equals("-0"))
            {
                return "0";
            }
            return formatted;
        }
    }

    private boolean canParseAsNumber(String s)
    {
        if (s == null || s.isEmpty() || s.equals("-") || s.equals(".") || s.equals("-.")) return false;
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

    private void displayError(String message)
    {
        displayField.setText(message);
        historyLog = "";
        currentInputValue = "0";
        firstOperand = 0;
        pendingOperator = "";
        isAwaitingSecondOperand = false;
        resultJustDisplayed = true;
    }

    private boolean isDisplayShowingError(String displayText)
    {
        if (displayText == null)
        {
            return false;
        }
        String text = displayText.toLowerCase();
        return text.contains("错误") || text.contains("error") || text.contains("nan") ||
                text.contains("溢出") || text.contains("未定义") || text.contains("除数不能为零") ||
                text.contains("无效输入") || text.contains("模数不能为零");
    }
}