package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.model.Currency;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;

/**
 * The Bakong KHQR payment card: red header with the KHQR wordmark, receiver name, amount +
 * currency, and the QR code — built to the official KHQR Card Guideline: 20:29 card ratio,
 * {@code #E1232E} header at 12% of card height, left-aligned Nunito-style typography sized
 * relative to card height (name/currency 3%, amount 6.5%), a dashed divider, 10% left/right and
 * 8% top/bottom QR margins, and a currency badge centered on the QR.
 */
public final class KhqrCardView extends StackPane {

    private static final String HEADER_RED = "#E1232E";
    private static final String BODY_TEXT = "#1A1A1A";
    private static final String MUTED_TEXT = "#8A8A8A";
    private static final String DIVIDER_COLOR = "#D8D8D8";
    private static final double RATIO_WIDTH = 20;
    private static final double RATIO_HEIGHT = 29;

    private final double cardWidth;
    private final double cardHeight;
    private final Text nameText = new Text();
    private final Text amountText = new Text();
    private final Text currencyText = new Text();
    private final Text badgeSymbol = new Text();
    private final ImageView qrImageView = new ImageView();

    public KhqrCardView(double width) {
        this.cardWidth = width;
        this.cardHeight = width * RATIO_HEIGHT / RATIO_WIDTH;
        build();
        setReceiverName("");
        setAmount("", Currency.USD);
    }

    public ImageView qrImageView() {
        return qrImageView;
    }

    public void setReceiverName(String name) {
        nameText.setText(StringUtils.isBlank(name) ? " " : name);
    }

    public void setAmount(String rawAmount, Currency currency) {
        amountText.setText(formatAmount(rawAmount));
        currencyText.setText(currency.display());
        badgeSymbol.setText(currency.symbol());
    }

    private static String formatAmount(String rawAmount) {
        if (StringUtils.isBlank(rawAmount)) {
            return "0";
        }
        try {
            return new DecimalFormat("#,##0.##").format(Double.parseDouble(rawAmount));
        } catch (NumberFormatException e) {
            return rawAmount;
        }
    }

    private void build() {
        setMinSize(cardWidth, cardHeight);
        setMaxSize(cardWidth, cardHeight);
        setPrefSize(cardWidth, cardHeight);

        double radius = cardWidth * 0.06;
        Rectangle clip = new Rectangle(cardWidth, cardHeight);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        setClip(clip);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.10));
        shadow.setRadius(16);
        shadow.setOffsetX(0);
        shadow.setOffsetY(0);
        setEffect(shadow);
        setStyle("-fx-background-color: white;");

        VBox layout = new VBox(buildHeader(), buildBody());
        layout.setPrefSize(cardWidth, cardHeight);
        getChildren().add(layout);
    }

    private HBox buildHeader() {
        double headerHeight = cardHeight * 0.12;
        HBox header = new HBox(SvgIcon.load("/khqr-assets/KHQR-logo-white.svg", headerHeight * 0.42));
        header.setAlignment(Pos.CENTER);
        header.setMinHeight(headerHeight);
        header.setMaxHeight(headerHeight);
        header.setPrefSize(cardWidth, headerHeight);
        header.setStyle("-fx-background-color: " + HEADER_RED + ";");
        return header;
    }

    private VBox buildBody() {
        double sideMargin = cardWidth * 0.10;
        double topBottomMargin = cardHeight * 0.08;

        nameText.setFont(Font.font(null, FontWeight.SEMI_BOLD, cardHeight * 0.045));
        nameText.setFill(Color.web(MUTED_TEXT));

        amountText.setFont(Font.font(null, FontWeight.EXTRA_BOLD, cardHeight * 0.065));
        amountText.setFill(Color.web(BODY_TEXT));
        currencyText.setFont(Font.font(null, FontWeight.SEMI_BOLD, cardHeight * 0.03));
        currencyText.setFill(Color.web(MUTED_TEXT));
        HBox amountRow = new HBox(cardWidth * 0.02, amountText, currencyText);
        amountRow.setAlignment(Pos.BASELINE_LEFT);

        double qrSize = cardWidth - 2 * sideMargin;
        Line divider = new Line(0, 0, qrSize, 0);
        divider.setStroke(Color.web(DIVIDER_COLOR));
        divider.getStrokeDashArray().addAll(4.0, 4.0);

        qrImageView.setPreserveRatio(true);
        qrImageView.setFitWidth(qrSize);
        qrImageView.setFitHeight(qrSize);

        double badgeRadius = qrSize * 0.09;
        Circle badge = new Circle(badgeRadius);
        badge.setFill(Color.web(BODY_TEXT));
        badgeSymbol.setFill(Color.WHITE);
        badgeSymbol.setFont(Font.font(null, FontWeight.BOLD, badgeRadius * 0.95));
        StackPane qrStack = new StackPane(qrImageView, badge, badgeSymbol);
        qrStack.setAlignment(Pos.CENTER);

        VBox body = new VBox(cardHeight * 0.02, nameText, amountRow, divider, qrStack);
        body.setAlignment(Pos.TOP_LEFT);
        body.setPadding(new Insets(topBottomMargin, sideMargin, topBottomMargin, sideMargin));
        body.setStyle("-fx-background-color: white;");
        VBox.setVgrow(body, Priority.ALWAYS);
        return body;
    }
}
