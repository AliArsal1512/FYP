/**
 * Test file to verify inheritance display in AST
 */
public class InheritanceTest {
    
    // Parent class
    class Animal {
        private String name;
        private int age;
        
        public Animal(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public void eat() {
            System.out.println("Animal is eating");
        }
        
        public void sleep() {
            System.out.println("Animal is sleeping");
        }
    }
    
    // Child class extending Animal
    class Dog extends Animal {
        private String breed;
        
        public Dog(String name, int age, String breed) {
            super(name, age);
            this.breed = breed;
        }
        
        public void bark() {
            System.out.println("Dog is barking");
        }
        
        @Override
        public void eat() {
            System.out.println("Dog is eating dog food");
        }
    }
    
    // Another child class extending Animal
    class Cat extends Animal {
        private boolean isIndoor;
        
        public Cat(String name, int age, boolean isIndoor) {
            super(name, age);
            this.isIndoor = isIndoor;
        }
        
        public void meow() {
            System.out.println("Cat is meowing");
        }
    }
    
    // Grandchild class extending Dog
    class Puppy extends Dog {
        private int monthsOld;
        
        public Puppy(String name, int age, String breed, int monthsOld) {
            super(name, age, breed);
            this.monthsOld = monthsOld;
        }
        
        public void play() {
            System.out.println("Puppy is playing");
        }
    }
    
    // Another parent class (no inheritance)
    class Vehicle {
        private String brand;
        private int year;
        
        public Vehicle(String brand, int year) {
            this.brand = brand;
            this.year = year;
        }
        
        public void start() {
            System.out.println("Vehicle is starting");
        }
    }
    
    // Child of Vehicle
    class Car extends Vehicle {
        private int doors;
        
        public Car(String brand, int year, int doors) {
            super(brand, year);
            this.doors = doors;
        }
        
        public void drive() {
            System.out.println("Car is driving");
        }
    }
    
    public static void main(String[] args) {
        InheritanceTest test = new InheritanceTest();
        Dog dog = test.new Dog("Buddy", 3, "Golden Retriever");
        Cat cat = test.new Cat("Whiskers", 2, true);
        Puppy puppy = test.new Puppy("Max", 1, "Labrador", 6);
        Car car = test.new Car("Toyota", 2020, 4);
        
        dog.eat();
        dog.bark();
        cat.eat();
        cat.meow();
        puppy.play();
        car.start();
        car.drive();
    }
}
