public class user{
    public String nick;
    public int state; //0-init, 1-outside, 2-inside
    public String room;
    public String buffer;

    public user()
    {
	state = 0;
	buffer = "";
	nick = "";
    }

    public void newnick(String name)
    {
	nick = name;
    }

    public void changeroom(String sala)
    {
	state = 2;
	room = sala;
    }

    public void leaveroom()
    {
	state = 1;
    }

    public void addbuff(String message)
    {
	buffer += message;
    }
    
    public String getbuffer()
    {
	String t = buffer;
	buffer = "";
	return t;
    }
    
    public String getnick()
    {
	return nick;
    }

    public String getroom()
    {
	return room;
    }

    public int getst()
    {
	return state;
    }
}
