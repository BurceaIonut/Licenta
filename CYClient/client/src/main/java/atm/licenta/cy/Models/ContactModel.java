package atm.licenta.cy.Models;

public class ContactModel {
    private String uid;
    private String name;
    private String status;
    public ContactModel(String uid, String name, String status) {
        this.uid = uid;
        this.name = name;
        this.status = status;
    }

    public String getUid(){
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
