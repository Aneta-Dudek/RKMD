package DAO;

import Model.Client;
import Model.User;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientsDatabaseDAO implements ClientsDAO {

    private String url;
    private String user;
    private String password;
  
    private List<User> ClientList;


    public ClientsDatabaseDAO() throws IOException {
        Properties prop = LoginData.readProperties("src/main/resources/database.properties");
        url = prop.getProperty("db.url");
        user = prop.getProperty("db.user");
        password = prop.getProperty("db.passwd");
    }


    public void updateDB(String query) {
        try {
            Connection con = DriverManager.getConnection(url, user, password);
            PreparedStatement pst = con.prepareStatement(query);
            pst.executeUpdate();
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getActualDate() {
        java.util.Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private void deleteFromAccountDetails(Integer user_ID) {
        String deleteStatement = String.format("DELETE FROM accountdetails WHERE accountdetails_id = %d",
                user_ID);
        updateDB(deleteStatement);
    }

    private void addClientToAccountDetails(String[] clientToAdd) {
        String date = getActualDate();
        String AddToAccountDetailsStatement = String.format("INSERT INTO accountdetails VALUES (DEFAULT, '%s', '%s', '%s')",
                date,
                clientToAdd[3],
                clientToAdd[2]);
        updateDB(AddToAccountDetailsStatement);
    }

    private void updateClientsAccountDetails(Integer acc_ID, String[] newAttributes) {
        String updateStatement = String.format("UPDATE accountdetails SET login = '%s', password = '%s' WHERE accountdetails_id = %d",
                newAttributes[3],
                newAttributes[4],
                acc_ID);
        updateDB(updateStatement);
    }


    @Override
    public void getAllClients() {
        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pst = con.prepareStatement("select user_id, first_name, last_name, login, password from user_table inner join accountdetails on user_table.account_details_id=accountdetails.accountdetails_id where admin_user = '0'");
             ResultSet rs = pst.executeQuery()) {

            int attributesNumber = rs.getMetaData().getColumnCount();
            ClientList = new ArrayList<>();
            String[] adminAttributes = new String[attributesNumber];

            while (rs.next()) {
                for (int index = 0; index < attributesNumber; index++) {
                    adminAttributes[index] = rs.getString(index + 1);
                }
                Client client = new Client(adminAttributes);
                ClientList.add(client);
                con.close();
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(AdminDatabaseDAO.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }


    @Override
    public void addClient(String[] clientToAdd) {
        addClientToAccountDetails(clientToAdd);
        String AddToUser_tableStatement = String.format("INSERT INTO User_table VALUES (DEFAULT, '%s', '%s', '0', DEFAULT)",
                clientToAdd[0],
                clientToAdd[1]);
        updateDB(AddToUser_tableStatement);
    }

    @Override
    public void updateClient(Integer user_ID, String[] newAttributes) {
        updateClientsAccountDetails(user_ID, newAttributes);        //since accountdetail_ID will be always same as user_ID
        String updateStatement = String.format("UPDATE user_table SET first_name = '%s', last_name = '%s' WHERE user_id = %d",
                newAttributes[1],
                newAttributes[2],
                user_ID);
        updateDB(updateStatement);
    }

    @Override
    public void deleteClient(Integer user_ID) {
        String deleteFromUserStatement = String.format("DELETE FROM User_table WHERE user_id = '%d'",
                user_ID);
        updateDB(deleteFromUserStatement);
        deleteFromAccountDetails(user_ID);
    }

    @Override
    public List<User> getClientList() {
        return ClientList;
    }

    @Override
    public boolean checkIsClient(String login, String password) {
        try (Connection con = DriverManager.getConnection(this.url, this.user, this.password);
             PreparedStatement pst = con.prepareStatement(
                     "select user_id, first_name, last_name, login, password from user_table inner join accountdetails on user_table.account_details_id=accountdetails.accountdetails_id where admin_user = '0' and login = ? and password = ?");
             ) {
            pst.setString(1, login);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            if(rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(AdminDatabaseDAO.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return false;
    }

    @Override
    public Client getClient(String login, String password) {
        try (Connection con = DriverManager.getConnection(this.url, this.user, this.password);
             PreparedStatement pst = con.prepareStatement(
                     "select user_id, first_name, last_name, login, password from user_table inner join accountdetails on user_table.account_details_id=accountdetails.accountdetails_id where admin_user = '0' and login = ? and password = ?");
        ) {
            pst.setString(1, login);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            int attributesNumber = rs.getMetaData().getColumnCount();

            String[] adminAttributes = new String[attributesNumber];

            if (rs.next()) {
                for (int index = 0; index < attributesNumber; index++) {
                    adminAttributes[index] = rs.getString(index + 1);
                }
                return new Client(adminAttributes);
            }
        } catch (SQLException ex) {
            Logger lgr = Logger.getLogger(AdminDatabaseDAO.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return null;
    }

}