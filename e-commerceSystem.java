import java.util.*;

abstract class Product {
    String name;
    int quantity;
    double price;

    Product(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    String getName() {
        return name;
    }

    double getPrice() {
        return price;
    }

    int getQuantity() {
        return quantity;
    }

    void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    abstract boolean requiresShipping();

    abstract boolean canExpire();

    void reduceQuantity(int amount) {
        quantity -= amount;
    }
}

interface Expirable {
    boolean isExpired();
    Date getExpiryDate();
}

interface Shippable {
    String getName();
    double getWeight();
}

class ExpirableProduct extends Product implements Expirable, Shippable {
    private Date expiryDate;
    private double weight;

    ExpirableProduct(String name, int quantity, double price, double weight, Date expiryDate) {
        super(name, quantity, price);
        this.weight = weight;
        this.expiryDate = expiryDate;
    }

    boolean requiresShipping() {
        return weight > 0;
    }

    boolean canExpire() {
        return true;
    }

    String getName() {
        return name;
    }

    double getWeight() {
        return weight;
    }

    boolean isExpired() {
        return expiryDate != null && new Date().after(expiryDate);
    }

    Date getExpiryDate() {
        return expiryDate;
    }
}

class ShippableProduct extends Product implements Shippable {
    private double weight;

    ShippableProduct(String name, int quantity, double price, double weight) {
        super(name, quantity, price);
        this.weight = weight;
    }

    boolean requiresShipping() {
        return weight > 0;
    }

    boolean canExpire() {
        return false;
    }

    String getName() {
        return name;
    }

    double getWeight() {
        return weight;
    }
}

class NonShippableProduct extends Product {
    NonShippableProduct(String name, int quantity, double price) {
        super(name, quantity, price);
    }

    boolean requiresShipping() {
        return false;
    }

    boolean canExpire() {
        return false;
    }
}

class Cart {
    Map<Product, Integer> items = new HashMap<>();

    void add(Product item, int quantity) {
        if (item == null || quantity <= 0)
            throw new IllegalArgumentException("Invalid item or quantity");
        if (item.getQuantity() < quantity)
            throw new IllegalStateException("Out of stock: " + item.getName());
        if (item instanceof Expirable && ((Expirable) item).isExpired())
            throw new IllegalStateException("Expired: " + item.getName());
        items.put(item, items.getOrDefault(item, 0) + quantity);
    }
}

class Customer {
    private String name;
    private double balance;
    private Cart cart = new Cart();

    Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    Cart getCart() {
        return cart;
    }

    double getBalance() {
        return balance;
    }

    void deductBalance(double amount) {
        balance -= amount;
    }
}

class ShippingService {
    void ship(List<Shippable> items) {
        System.out.println("** Shipment notice **");
        double totalWeight = 0;
        for (Shippable item : items) {
            System.out.println(item.getName() + " " + item.getWeight() + "g");
            totalWeight += item.getWeight();
        }
        System.out.println("Total package weight " + (totalWeight / 1000) + "kg");
    }
}

class Checkout {
    double SHIPPING_RATE = 30.0;

    void processOrder(Customer customer) {
        Cart cart = customer.getCart();
        if (cart.items.isEmpty())
            throw new IllegalStateException("Cart is empty");

        for (Map.Entry<Product, Integer> entry : cart.items.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            if (product.getQuantity() < quantity)
                throw new IllegalStateException("Not enough stock for " + product.getName());
            if (product.canExpire() && ((Expirable) product).isExpired())
                throw new IllegalStateException(product.getName() + " has expired");
        }

        double subtotal = 0;
        List<Shippable> shippableItems = new ArrayList<>();
        for (Map.Entry<Product, Integer> entry : cart.items.entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            subtotal += p.getPrice() * qty;
            if (p.requiresShipping() && p instanceof Shippable) {
                Shippable product = (Shippable) p;
                shippableItems.add(new Shippable() {
                    public String getName() {
                        return qty + "x " + product.getName();
                    }

                    public double getWeight() {
                        return product.getWeight() * qty;
                    }
                });
            }
        }

        double shipping = shippableItems.stream().mapToDouble(Shippable::getWeight).sum() / 1000 * SHIPPING_RATE;
        double total = subtotal + shipping;

        if (customer.getBalance() < total)
            throw new IllegalStateException("Insufficient funds");

        for (Map.Entry<Product, Integer> entry : cart.items.entrySet()) {
            entry.getKey().reduceQuantity(entry.getValue());
        }
        customer.deductBalance(total);

        System.out.println("** Checkout receipt **");
        for (Map.Entry<Product, Integer> entry : cart.items.entrySet()) {
            System.out.println(entry.getValue() + "x " + entry.getKey().getName() + " " +
                    (entry.getKey().getPrice() * entry.getValue()));
        }
        System.out.println("----------------------");
        System.out.println("Subtotal " + subtotal);
        System.out.println("Shipping " + shipping);
        System.out.println("Amount " + total);
        System.out.println("Balance: " + customer.getBalance());

        if (!shippableItems.isEmpty())
            new ShippingService().ship(shippableItems);
    }
}

public class EcommerceSystem {
    public static void main(String[] args) {
        ExpirableProduct cheese = new ExpirableProduct("Cheese", 5, 100, 200,
                new Date(System.currentTimeMillis() + 86400000));
        ExpirableProduct biscuits = new ExpirableProduct("Biscuits", 3, 150, 700,
                new Date(System.currentTimeMillis() + 2 * 86400000));
        ShippableProduct tv = new ShippableProduct("TV", 2, 1000, 5000);
        NonShippableProduct scratchCard = new NonShippableProduct("Scratch Card", 10, 50);

        Customer customer1 = new Customer("mahmoud", 1000);
        Cart cart1 = customer1.getCart();
        Checkout checkout = new Checkout();
        cart1.add(cheese, 2);
        cart1.add(biscuits, 1);
        cart1.add(scratchCard, 1);
        checkout.processOrder(customer1);

        Customer customer2 = new Customer("omar", 1000);
        Cart cart2 = customer2.getCart();
        checkout.processOrder(customer2);

        Customer customer3 = new Customer("ahmed", 100);
        Cart cart3 = customer3.getCart();
        cart3.add(tv, 2);
        checkout.processOrder(customer3);

        Customer customer4 = new Customer("moaaz", 1000);
        Cart cart4 = customer4.getCart();
        cart4.add(cheese, 10);
        checkout.processOrder(customer4);

        
    }
}
