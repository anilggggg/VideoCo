package view.shoppanels;

import model.Address;
import model.Cart;
import model.Model;
import model.Movie;
import model.User;
import model.payments.CreditCard;
import model.payments.LoyaltyPoints;
import services.PaymentService;
import view.cards.ShopCards;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Map;

public class CartPanel extends JPanel implements ActionListener {

    private ShopCards cards;

    private JButton removeItem;
    private JButton clearCart;
    private JButton checkout;
    private JLabel totalCost;
    private double customerTotal;
    private JLabel displayLoyaltyPoints;
    private int customerLoyaltyPoints;
    private JLabel loyaltyPointsLabel;

    // payment
    private ButtonGroup paymentServices;
    private JRadioButton loyaltyPointsOption;
    private JRadioButton creditCardOption;


    // error messages
    private static final String EMPTY_CART_ERROR = "No items in the cart.";
    private static final String NO_ITEM_SELECTED_ERROR = "No item selected.";
    private static final String NO_ADDRESS_ERROR = "No address on file. Please update your address in Account Details.";
    private static final String INVALID_PAYMENT_ERROR = "Invalid Payment. Please try again.";

    /**
     * Components for displaying data
     */
    private final JTable table;
    private final JScrollPane scrollPane;

    public CartPanel(ShopCards cards) {
        this.cards = cards;
        setLayout(new BorderLayout(20, 10));

        JLabel paymentLabel = new JLabel("Choose your payment method:");
        loyaltyPointsOption = new JRadioButton("Loyalty Points");
        loyaltyPointsOption.setMnemonic(KeyEvent.VK_C);
        loyaltyPointsOption.setActionCommand("loyaltyPoints");
        creditCardOption = new JRadioButton("Credit Card");
        creditCardOption.setMnemonic(KeyEvent.VK_C);
        creditCardOption.setActionCommand("creditCard");
        paymentServices = new ButtonGroup();
        paymentServices.add(loyaltyPointsOption);
        paymentServices.add(creditCardOption);

        removeItem = new JButton("Remove Item");
        removeItem.addActionListener(this);
        removeItem.setActionCommand("removeItem");

        clearCart = new JButton("Clear Cart");
        clearCart.addActionListener(this);
        clearCart.setActionCommand("clearCart");

        checkout = new JButton("Place Order");
        checkout.addActionListener(this);
        checkout.setActionCommand("checkout");

        loyaltyPointsLabel = new JLabel("Loyalty Points:");
        customerLoyaltyPoints = Model.getUserService().getLoggedInUser().getLoyaltyPoints();
        displayLoyaltyPoints = new JLabel(String.valueOf(customerLoyaltyPoints));

        table = new JTable();
        updateCart();

        totalCost = new JLabel("");

        scrollPane = new JScrollPane(table);

        JPanel northBar = new JPanel();
        northBar.add(removeItem);
        northBar.add(clearCart);
        northBar.add(loyaltyPointsLabel);
        northBar.add(displayLoyaltyPoints);

        JPanel southBar = new JPanel();
        southBar.add(paymentLabel);
        southBar.add(creditCardOption);
        southBar.add(loyaltyPointsOption);
        southBar.add(totalCost);
        southBar.add(checkout);

        add(scrollPane, BorderLayout.CENTER);
        add(northBar, BorderLayout.NORTH);
        add(new JPanel(), BorderLayout.WEST);
        add(new JPanel(), BorderLayout.EAST);
        add(southBar, BorderLayout.SOUTH);
        setVisible(true);
    }

    public void updateCart() {
        Cart userCart = Model.getUserService().getLoggedInUser().getCart();
        DefaultTableModel tmodel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        String[][] data = new String[userCart.getMoviesInCart().size()][4];
        String[] column = {"BARCODE", "TITLE", "PRICE", "QUANTITY"};
        int i = 0;
        double tempCost = 0;
        for (Map.Entry<Movie, Integer> entry : userCart.getMoviesInCart().entrySet()) {
            data[i][0] = entry.getKey().getBarcode();
            data[i][1] = entry.getKey().getTitle();
            data[i][2] = String.valueOf(entry.getKey().getPrice());
            data[i][3] = String.valueOf(entry.getValue());
            tempCost += entry.getKey().getPrice() * entry.getValue();
            i++;
        }
        customerTotal = tempCost;
        if (totalCost == null) {
            totalCost = new JLabel("");
        }
        totalCost.setText(String.format("Total: %.2f", customerTotal));
        tmodel.setDataVector(data,column);
        table.setModel(tmodel);
    }

    private void updateView() {
        updateCart();
        displayLoyaltyPoints.setText(String.valueOf(Model.getUserService().getLoggedInUser().getLoyaltyPoints()));
        cards.getStorePanel().displayAllMovies();
        cards.getOrderPanel().updateTable();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("removeItem")) {
            int[] selected = table.getSelectedRows();
            if (selected.length == 0) {
                JOptionPane.showMessageDialog(this, NO_ITEM_SELECTED_ERROR);
            } else {
                Cart userCart = Model.getUserService().getLoggedInUser().getCart();
                for (int row : selected) {
                    Movie m = new Movie();
                    m.setBarcode((String) table.getValueAt(row, 0));
                    m.setTitle((String) table.getValueAt(row, 1));
                    userCart.removeMovieFromCart(m);
                    updateView();
                }
            }
        } else if (e.getActionCommand().equals("clearCart")) {
            Model.getUserService().getLoggedInUser().getCart().clearCart();
            updateCart();
        } else if (e.getActionCommand().equals("checkout")) {
            User u = Model.getUserService().getLoggedInUser();
            Address userAddress = Model.getAddressService().getAddress(u.getUsername());
            ButtonModel buttonModel = paymentServices.getSelection();
            if (buttonModel != null) {
                if (u.getCart().getMoviesInCart().size() == 0) {
                    JOptionPane.showMessageDialog(this, EMPTY_CART_ERROR);
                } else if (userAddress == null) {
                    JOptionPane.showMessageDialog(this, NO_ADDRESS_ERROR, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    // create order
                    PaymentService paymentMethod = null;
                    if (buttonModel.getActionCommand().equals("loyaltyPoints")) {
                        paymentMethod = new LoyaltyPoints(u.getLoyaltyPoints());
                    } else if (buttonModel.getActionCommand().equals("creditCard")) {
                        paymentMethod = new CreditCard();
                    }
                    boolean paymentAccepted = Model.getOrderService().createOrder(u.getCart(), paymentMethod, userAddress);
                    if (paymentAccepted) {
                        JOptionPane.showMessageDialog(this, "Order Created!");
                        u.getCart().clearCart();
                        updateView();
                    } else {
                        JOptionPane.showMessageDialog(this, "Payment Not Accepted", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, INVALID_PAYMENT_ERROR, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}