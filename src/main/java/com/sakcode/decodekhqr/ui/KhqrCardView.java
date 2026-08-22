package com.sakcode.decodekhqr.ui;

import com.sakcode.decodekhqr.model.Currency;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;

/**
 * The Bakong KHQR payment card, laid out to the official KHQR Card Appearance Guideline.
 *
 * <p>Every dimension is a fraction of the card height {@code H} (the card itself is a fixed
 * 20:29 ratio): a {@code #E1232E} header of {@code 0.12H} closed by a 45&deg; ribbon tail on the
 * right, left/right margins of {@code 0.10H}, top/bottom QR margins of {@code 0.08H}, a full-bleed
 * dashed divider, and the currency badge artwork centered on the QR. Text baselines are placed absolutely
 * rather than stacked, so the card renders identically at any width.
 *
 * <p>The card hides itself whenever {@link #qrImageView()} holds no image, so nothing is shown
 * before a QR is generated or decoded, or after the form is cleared.
 */
public final class KhqrCardView extends StackPane {

    private static final String HEADER_RED = "#E1232E";
    private static final String AMOUNT_TEXT = "#111111";
    private static final String NAME_TEXT = "#333333";
    private static final String CURRENCY_TEXT = "#1A1A1A";
    private static final String DIVIDER_COLOR = "#C9C9C9";

    private static final double RATIO_WIDTH = 20;
    private static final double RATIO_HEIGHT = 29;

    /** Guideline margins, all expressed as a fraction of the card height. */
    private static final double HEADER_HEIGHT = 0.12;
    private static final double SIDE_MARGIN = 0.10;
    private static final double QR_MARGIN = 0.08;

    /** Text baselines, measured down from the header bottom edge as a fraction of card height. */
    private static final double NAME_BASELINE = 0.081;
    private static final double AMOUNT_BASELINE = 0.171;

    /** Font sizes, as a fraction of the card height. */
    private static final double AMOUNT_FONT = 0.060;
    private static final double CURRENCY_FONT = 0.040;

    /** Currency badge disc diameter as a fraction of the QR, and its share of the artwork canvas. */
    private static final double BADGE_DIAMETER = 0.176;
    private static final double BADGE_DISC_FILL = 0.84;

    private static final Map<Currency, Image> BADGES = new EnumMap<>(Currency.class);

    private final double cardWidth;
    private final double cardHeight;
    private final double sideMargin;
    private final Text nameText = new Text();
    private final Text amountText = new Text();
    private final Text currencyText = new Text();
    private final ImageView badgeView = new ImageView();
    private final ImageView qrImageView = new ImageView();

    public KhqrCardView(double width) {
        this.cardWidth = width;
        this.cardHeight = width * RATIO_HEIGHT / RATIO_WIDTH;
        this.sideMargin = cardHeight * SIDE_MARGIN;
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
        amountText.setText(formatAmount(rawAmount, currency));
        currencyText.setText(currencySymbol(currency));
        badgeView.setImage(badgeImage(currency));
        layoutCurrency();
    }

    /** The card renders the currency as its symbol rather than its ISO code. */
    private static String currencySymbol(Currency currency) {
        return currency == Currency.KHR ? "៛" : "$";
    }

    /**
     * Groups the amount in thousands. A whole amount stays whole ({@code 100} renders as
     * {@code 100}), but once there are decimals they are padded out to the currency's minor units,
     * so {@code 100.3} renders as {@code 100.30} in USD. Extra entered precision is kept.
     */
    private static String formatAmount(String rawAmount, Currency currency) {
        if (StringUtils.isBlank(rawAmount)) {
            return "0";
        }
        try {
            BigDecimal amount = new BigDecimal(rawAmount.trim());
            int decimals = amount.scale() <= 0 ? 0 : Math.max(amount.scale(), currency.minorUnits());
            DecimalFormat format = new DecimalFormat("#,##0");
            format.setMinimumFractionDigits(decimals);
            format.setMaximumFractionDigits(decimals);
            return format.format(amount);
        } catch (NumberFormatException e) {
            return rawAmount;
        }
    }

    private void build() {
        setMinSize(cardWidth, cardHeight);
        setMaxSize(cardWidth, cardHeight);
        setPrefSize(cardWidth, cardHeight);

        double radius = cardWidth * 0.045;
        setStyle("-fx-background-color: white;"
                + " -fx-background-radius: " + radius + ";");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.color(0, 0, 0, 0.10));
        shadow.setRadius(16);
        shadow.setOffsetX(0);
        shadow.setOffsetY(0);
        setEffect(shadow);

        Pane content = new Pane();
        content.setPrefSize(cardWidth, cardHeight);
        content.getChildren().addAll(buildHeader(), buildText(), buildDivider(), buildQr());

        Rectangle clip = new Rectangle(cardWidth, cardHeight);
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        content.setClip(clip);

        getChildren().add(content);

        // The card only means anything once a QR exists: it stays hidden — and takes no layout
        // space — until one is generated or decoded, and disappears again when the form is cleared.
        visibleProperty().bind(qrImageView.imageProperty().isNotNull());
        managedProperty().bind(visibleProperty());
    }

    /** The red banner plus its 45&deg; ribbon tail folding down past the right edge. */
    private Node buildHeader() {
        double headerHeight = cardHeight * HEADER_HEIGHT;
        double tail = sideMargin;

        Rectangle banner = new Rectangle(cardWidth, headerHeight, Color.web(HEADER_RED));
        Polygon ribbon = new Polygon(
                cardWidth - tail, headerHeight,
                cardWidth, headerHeight - 2,
                cardWidth, headerHeight + tail - 2);
        ribbon.setFill(Color.web(HEADER_RED));

        Node logo = SvgIcon.load("/khqr-assets/KHQR-logo-white.svg", headerHeight * 0.30);
        Bounds ink = logo.getBoundsInParent();
        logo.setLayoutX((cardWidth - ink.getWidth()) / 2 - ink.getMinX());
        logo.setLayoutY((headerHeight - ink.getHeight()) / 2 - ink.getMinY());

        return new Pane(banner, ribbon, logo);
    }

    /** Receiver name and amount, left-aligned on the side margin at fixed baselines. */
    private Node buildText() {
        double headerBottom = cardHeight * HEADER_HEIGHT;

        nameText.setFont(Font.font(null, FontWeight.NORMAL, cardHeight * CURRENCY_FONT));
        nameText.setFill(Color.web(NAME_TEXT));
        nameText.setX(sideMargin);
        nameText.setY(headerBottom + cardHeight * NAME_BASELINE);

        currencyText.setFont(Font.font(null, FontWeight.EXTRA_BOLD, cardHeight * AMOUNT_FONT));
        currencyText.setFill(Color.web(CURRENCY_TEXT));
        currencyText.setX(sideMargin);
        currencyText.setY(headerBottom + cardHeight * AMOUNT_BASELINE);

        amountText.setFont(Font.font(null, FontWeight.EXTRA_BOLD, cardHeight * AMOUNT_FONT));
        amountText.setFill(Color.web(AMOUNT_TEXT));
        amountText.setY(headerBottom + cardHeight * AMOUNT_BASELINE);

        return new Pane(nameText, currencyText, amountText);
    }

    /** Places the amount just after the leading currency code, which changes width by currency. */
    private void layoutCurrency() {
        amountText.setX(sideMargin + currencyText.getLayoutBounds().getWidth() + cardWidth * 0.02);
    }

    /** Full-bleed dashed rule, one QR margin above the QR. */
    private Node buildDivider() {
        double y = qrTop() - cardHeight * QR_MARGIN;
        Line divider = new Line(0, y, cardWidth, y);
        divider.setStroke(Color.web(DIVIDER_COLOR));
        divider.setStrokeWidth(1);
        divider.getStrokeDashArray().addAll(6.0, 5.0);
        return divider;
    }

    private double qrSize() {
        return cardWidth - 2 * sideMargin;
    }

    private double qrTop() {
        return cardHeight - cardHeight * QR_MARGIN - qrSize();
    }

    private Node buildQr() {
        double size = qrSize();
        qrImageView.setPreserveRatio(true);
        // The encoded QR is larger than the card box, so smooth the downscale — nearest-neighbour
        // drops whole module rows at these non-integer ratios.
        qrImageView.setSmooth(true);
        qrImageView.setFitWidth(size);
        qrImageView.setFitHeight(size);
        qrImageView.setX(sideMargin);
        qrImageView.setY(qrTop());
        // The encoded PNG carries its own quiet zone; the card supplies the margins itself, so
        // crop to the modules and let them fill the guideline box exactly.
        qrImageView.imageProperty().addListener((obs, old, image) -> cropQuietZone(image));

        // The badge artwork carries transparent padding around the disc, so the drawn canvas is
        // larger than the disc itself.
        double canvas = size * BADGE_DIAMETER / BADGE_DISC_FILL;
        badgeView.setPreserveRatio(true);
        badgeView.setFitWidth(canvas);
        badgeView.setFitHeight(canvas);
        badgeView.setX(sideMargin + (size - canvas) / 2);
        badgeView.setY(qrTop() + (size - canvas) / 2);

        return new Pane(qrImageView, badgeView);
    }

    /** Sets a viewport around the QR modules only, dropping the encoder's white quiet zone. */
    private void cropQuietZone(Image image) {
        if (image == null) {
            qrImageView.setViewport(null);
            return;
        }
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        int top = 0;
        while (top < height && isBlank(reader, 0, width, top, top + 1)) {
            top++;
        }
        if (top == height) {
            qrImageView.setViewport(null);
            return;
        }
        int bottom = height - 1;
        while (bottom > top && isBlank(reader, 0, width, bottom, bottom + 1)) {
            bottom--;
        }
        int left = 0;
        while (left < width && isBlank(reader, left, left + 1, top, bottom + 1)) {
            left++;
        }
        int right = width - 1;
        while (right > left && isBlank(reader, right, right + 1, top, bottom + 1)) {
            right--;
        }
        qrImageView.setViewport(new Rectangle2D(left, top, right - left + 1, bottom - top + 1));
    }

    private static boolean isBlank(PixelReader reader, int fromX, int toX, int fromY, int toY) {
        for (int y = fromY; y < toY; y++) {
            for (int x = fromX; x < toX; x++) {
                if ((reader.getArgb(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The currency badge artwork shipped with the KHQR assets, one PNG per currency. */
    private static Image badgeImage(Currency currency) {
        return BADGES.computeIfAbsent(currency, key -> {
            String path = "/khqr-assets/" + key.name() + ".png";
            InputStream in = KhqrCardView.class.getResourceAsStream(path);
            if (in == null) {
                throw new IllegalStateException("Missing currency badge asset: " + path);
            }
            return new Image(in);
        });
    }
}
